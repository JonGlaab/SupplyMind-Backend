package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.SupplierPaymentStatus;
import com.supplymind.platform_core.dto.core.payments.CreatePaymentIntentRequestDTO;
import com.supplymind.platform_core.model.core.SupplierPayment;
import com.supplymind.platform_core.repository.core.SupplierPaymentRepository;
import com.supplymind.platform_core.service.core.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SupplierPaymentScheduler {

    private final SupplierPaymentRepository supplierPaymentRepo;
    private final PaymentService paymentService;

    // every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        var due = supplierPaymentRepo.findByStatusAndScheduledForBefore(SupplierPaymentStatus.SCHEDULED, Instant.now());

        for (SupplierPayment sp : due) {
            execute(sp);
        }
    }

    private void execute(SupplierPayment sp) {
        sp.setStatus(SupplierPaymentStatus.PROCESSING);
        supplierPaymentRepo.save(sp);

        CreatePaymentIntentRequestDTO req = new CreatePaymentIntentRequestDTO();
        req.setPoId(sp.getPo().getPoId());
        req.setPaymentType("SUPPLIER_PAYMENT");
        req.setCurrency(sp.getCurrency());
        req.setAmountOverride(sp.getAmount()); // ✅ partial supported now

        var res = paymentService.createPaymentIntent(req);

        sp.setStripePaymentIntentId(res.getPaymentIntentId());
        supplierPaymentRepo.save(sp);

        // final PAID/FAILED will be set by webhook
    }
}

