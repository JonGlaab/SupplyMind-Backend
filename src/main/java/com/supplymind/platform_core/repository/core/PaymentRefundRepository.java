package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByStripeRefundId(String stripeRefundId);
}

