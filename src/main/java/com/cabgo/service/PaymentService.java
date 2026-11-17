package com.cabgo.service;

import com.cabgo.model.Payment;
import com.cabgo.model.Ride;
import com.cabgo.model.RideStatus;
import com.cabgo.model.User;
import com.cabgo.repo.PaymentRepository;
import com.cabgo.repo.RideRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/** Stripe PaymentIntent flow. Without Stripe keys it runs in DEMO mode so the app works out of the box. */
@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final RideRepository rides;
    private final SimpMessagingTemplate ws;

    @Value("${app.stripe.secret-key:}") private String secretKey;
    @Value("${app.stripe.publishable-key:}") private String publishableKey;
    @Value("${app.stripe.webhook-secret:}") private String webhookSecret;
    @Value("${app.stripe.currency:inr}") private String currency;

    public PaymentService(PaymentRepository payments, RideRepository rides, SimpMessagingTemplate ws) {
        this.payments = payments;
        this.rides = rides;
        this.ws = ws;
    }

    @PostConstruct
    void init() {
        if (enabled()) Stripe.apiKey = secretKey;
    }

    public boolean enabled() {
        return secretKey != null && !secretKey.isBlank() && publishableKey != null && !publishableKey.isBlank();
    }

    public String publishableKey() { return publishableKey; }
    public String currency() { return currency; }

    public Map<String, Object> createIntent(User rider, Long rideId) {
        Ride ride = owned(rider, rideId);
        Payment p = payments.findByRide(ride).orElseGet(() -> {
            Payment n = new Payment();
            n.setRide(ride);
            n.setAmount(ride.getFare());
            return n;
        });
        if ("PAID".equals(p.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already paid");
        if (!enabled()) {
            p.setPaymentMode("DEMO");
            payments.save(p);
            return Map.of("mode", "DEMO", "amount", ride.getFare());
        }
        try {
            PaymentIntent pi = PaymentIntent.create(PaymentIntentCreateParams.builder()
                    .setAmount(Math.round(ride.getFare() * 100))
                    .setCurrency(currency)
                    .putMetadata("rideId", String.valueOf(rideId))
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build())
                    .build());
            p.setPaymentMode("STRIPE");
            p.setStripeIntentId(pi.getId());
            payments.save(p);
            return Map.of("mode", "STRIPE", "clientSecret", pi.getClientSecret(), "amount", ride.getFare());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
        }
    }

    /** Server-side verification after the client confirms the card payment. */
    public Ride confirm(User rider, Long rideId) {
        Ride ride = owned(rider, rideId);
        Payment p = payments.findByRide(ride)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start the payment first"));
        if (!enabled()) {
            markPaid(p);
        } else {
            try {
                PaymentIntent pi = PaymentIntent.retrieve(p.getStripeIntentId());
                if (!"succeeded".equals(pi.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment not completed");
                }
                markPaid(p);
            } catch (StripeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
            }
        }
        return rides.findById(rideId).orElseThrow();
    }

    public void webhook(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad signature");
        }
        if ("payment_intent.succeeded".equals(event.getType())) {
            StripeObject o = event.getDataObjectDeserializer().getObject().orElse(null);
            if (o instanceof PaymentIntent pi) {
                payments.findByStripeIntentId(pi.getId()).ifPresent(this::markPaid);
            }
        }
    }

    private void markPaid(Payment p) {
        if ("PAID".equals(p.getStatus())) return;
        p.setStatus("PAID");
        p.setPaidAt(Instant.now());
        payments.save(p);
        Ride r = p.getRide();
        r.setPaid(true);
        rides.save(r);
        ws.convertAndSend("/topic/ride/" + r.getId(), Map.of("type", "PAID"));
    }

    private Ride owned(User rider, Long rideId) {
        Ride ride = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!ride.getRider().getId().equals(rider.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        if (ride.getStatus() != RideStatus.COMPLETED) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride not completed yet");
        return ride;
    }
}
