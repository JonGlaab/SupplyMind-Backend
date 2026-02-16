package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.returns.ApproveReturnDTO;
import com.supplymind.platform_core.dto.core.returns.CreateReturnRequestDTO;
import com.supplymind.platform_core.dto.core.returns.ReceiveReturnDTO;
import com.supplymind.platform_core.dto.core.returns.RefundReturnDTO;
import com.supplymind.platform_core.model.core.ReturnReceipt;
import com.supplymind.platform_core.model.core.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReturnService {
    ReturnRequest createReturn(CreateReturnRequestDTO dto);

    ReturnRequest approveReturn(Long returnId, ApproveReturnDTO dto);

    ReturnReceipt receiveReturn(Long returnId, ReceiveReturnDTO dto);

    ReturnRequest refundReturn(Long returnId, RefundReturnDTO dto);

    ReturnRequest cancelReturn(Long returnId, String cancelledBy);

    ReturnRequest getReturn(Long returnId);

    Page<ReturnRequest> getAllReturns(Pageable pageable);

    List<ReturnRequest> getReturnsByPoId(Long poId);
}