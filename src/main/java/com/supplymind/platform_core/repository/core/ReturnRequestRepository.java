package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByPo_PoId(Long poId);
}

