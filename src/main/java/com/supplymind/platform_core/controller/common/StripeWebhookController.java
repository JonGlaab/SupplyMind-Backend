package com.supplymind.platform_core.controller.common;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.supplymind.platform_core.model.core.Payment;
import com.supplymind.platform_core.repository.core.PaymentRepository;
import com.supplymind.platform_core.repository.core.SupplierInvoiceRepository;
import com.supplymind.platform_core.repository.core.SupplierPaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final PaymentRepository paymentRepo;

    private final SupplierPaymentRepository supplierPaymentRepo;
    private final SupplierInvoiceRepository supplierInvoiceRepo;


    @PostMapping("/stripe")
    @Transactional
    public String handleStripeWebhook(@RequestBody String payload,
                                      @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return "invalid signature";
        }

        switch (event.getType()) {

            case "payment_intent.succeeded" -> {
                PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);

                if (pi != null) {
                    paymentRepo.findByStripeId(pi.getId()).ifPresent(p -> {
                        p.setStatus("PAID");
                        p.setPaidAt(Instant.now());
                        paymentRepo.save(p);
                    });
                }
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);

                if (pi != null) {
                    paymentRepo.findByStripeId(pi.getId()).ifPresent(p -> {
                        p.setStatus("FAILED");
                        paymentRepo.save(p);
                    });
                }
            }

            // This event updates both partial and full refunds reliably
            case "charge.refunded" -> {
                Charge charge = (Charge) event.getDataObjectDeserializer()
                        .getObject().orElse(null);

                if (charge != null && charge.getPaymentIntent() != null) {
                    String paymentIntentId = charge.getPaymentIntent();

                    paymentRepo.findByStripeId(paymentIntentId).ifPresent(p -> {
                        BigDecimal refundedTotal = BigDecimal.valueOf(charge.getAmountRefunded()).movePointLeft(2);
                        p.setRefundedAmount(refundedTotal);

                        if (refundedTotal.compareTo(p.getAmount()) >= 0) {
                            p.setStatus("REFUNDED");
                        } else if (refundedTotal.compareTo(BigDecimal.ZERO) > 0) {
                            p.setStatus("PARTIALLY_REFUNDED");
                        }

                        paymentRepo.save(p);
                    });
                }
            }
        }

        return "ok";
    }
}
