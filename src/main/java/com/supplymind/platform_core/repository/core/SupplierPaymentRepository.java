package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.common.enums.SupplierPaymentStatus;
import com.supplymind.platform_core.model.core.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findBySupplier_SupplierId(Long supplierId);

    List<SupplierPayment> findByStatusAndScheduledForBefore(SupplierPaymentStatus status, Instant now);

    Optional<SupplierPayment> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<SupplierPayment> findByInvoice_InvoiceId(Long invoiceId);

    List<SupplierPayment> findByInvoice_InvoiceIdOrderBySupplierPaymentIdDesc(Long invoiceId);
    List<SupplierPayment> findBySupplier_SupplierIdOrderByCreatedAtDesc(Long supplierId);
}
