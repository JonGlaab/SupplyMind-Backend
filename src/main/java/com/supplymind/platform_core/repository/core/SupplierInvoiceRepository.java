package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.SupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {
    Optional<SupplierInvoice> findByPo_PoId(Long poId);
    List<SupplierInvoice> findBySupplier_SupplierId(Long supplierId);
}


