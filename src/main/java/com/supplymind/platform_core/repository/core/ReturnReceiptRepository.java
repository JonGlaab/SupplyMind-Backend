package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.ReturnReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnReceiptRepository extends JpaRepository<ReturnReceipt, Long> {
    List<ReturnReceipt> findByReturnRequest_Id(Long returnId);
}
