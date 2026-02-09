package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripeId(String stripeId);
}

