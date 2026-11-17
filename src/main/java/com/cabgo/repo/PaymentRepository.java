package com.cabgo.repo;

import com.cabgo.model.Payment;
import com.cabgo.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRide(Ride ride);
    Optional<Payment> findByStripeIntentId(String intentId);
}
