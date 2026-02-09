package com.supplymind.platform_core.service.impl.core;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.supplymind.platform_core.dto.core.payments.*;
import com.supplymind.platform_core.model.core.Payment;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.core.PaymentRepository;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.service.core.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final PurchaseOrderRepository poRepo;

    @Value("${stripe.currency:cad}")
    private String currency;

    @Override
    @Transactional
    public CreatePaymentIntentResponseDTO createPaymentIntent(CreatePaymentIntentRequestDTO dto) {
        PurchaseOrder po = poRepo.findById(dto.getPoId())
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + dto.getPoId()));

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (dto.getPaymentType() == null || dto.getPaymentType().isBlank()) {
            throw new IllegalArgumentException("paymentType is required");
        }

        Payment p = new Payment();
        p.setPo(po);
        p.setAmount(dto.getAmount());
        p.setStatus("PENDING");
        p.setPaymentType(dto.getPaymentType());
        p.setCurrency(currency);
        p.setRefundedAmount(BigDecimal.ZERO);
        p = paymentRepo.save(p);

        long amountCents = dto.getAmount().movePointRight(2).longValueExact();

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency)
                    .putMetadata("poId", String.valueOf(po.getPoId()))
                    .putMetadata("paymentId", String.valueOf(p.getId()))
                    .build();

            RequestOptions opts = RequestOptions.builder()
                    .setIdempotencyKey("pi_" + p.getId())
                    .build();

            PaymentIntent pi = PaymentIntent.create(params, opts);

            // store PaymentIntent id (pi_...) in stripe_id
            p.setStripeId(pi.getId());
            paymentRepo.save(p);

            return new CreatePaymentIntentResponseDTO(p.getId(), pi.getId(), pi.getClientSecret());
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error creating PaymentIntent: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void requestRefund(CreateRefundRequestDTO dto) {
        Payment p = paymentRepo.findById(dto.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + dto.getPaymentId()));

        if (p.getStripeId() == null || p.getStripeId().isBlank()) {
            throw new IllegalStateException("Payment has no Stripe PaymentIntent id in stripe_id.");
        }

        if (!"PAID".equals(p.getStatus()) && !"PARTIALLY_REFUNDED".equals(p.getStatus())) {
            throw new IllegalStateException("Payment must be PAID or PARTIALLY_REFUNDED to refund.");
        }

        BigDecimal refunded = p.getRefundedAmount() == null ? BigDecimal.ZERO : p.getRefundedAmount();
        BigDecimal refundable = p.getAmount().subtract(refunded);

        BigDecimal refundAmount = (dto.getAmount() == null) ? refundable : dto.getAmount();

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("refund amount must be > 0");
        if (refundAmount.compareTo(refundable) > 0) throw new IllegalArgumentException("refund amount exceeds refundable balance");

        long refundCents = refundAmount.movePointRight(2).longValueExact();

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(p.getStripeId())
                    .setAmount(refundCents) // supports partial + full
                    .build();

            RequestOptions opts = RequestOptions.builder()
                    .setIdempotencyKey("rf_" + p.getId() + "_" + UUID.randomUUID())
                    .build();

            Refund.create(params, opts);

            // Don't finalize DB status here — webhook will set final status.
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error creating refund: " + e.getMessage(), e);
        }
    }
}
