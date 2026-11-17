package com.cabgo.web;

import com.cabgo.model.Ride;
import com.cabgo.model.User;
import com.cabgo.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/{rideId}/intent")
    @PreAuthorize("hasRole('RIDER')")
    public Map<String, Object> intent(@AuthenticationPrincipal User u, @PathVariable Long rideId) {
        return payments.createIntent(u, rideId);
    }

    @PostMapping("/{rideId}/confirm")
    @PreAuthorize("hasRole('RIDER')")
    public Ride confirm(@AuthenticationPrincipal User u, @PathVariable Long rideId) {
        return payments.confirm(u, rideId);
    }

    /** Stripe webhook: signature-verified, marks the payment as PAID. */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String signature) {
        payments.webhook(payload, signature);
        return ResponseEntity.ok("ok");
    }
}
