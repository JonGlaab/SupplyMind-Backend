package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.returns.ApproveReturnDTO;
import com.supplymind.platform_core.dto.core.returns.CreateReturnRequestDTO;
import com.supplymind.platform_core.dto.core.returns.ReceiveReturnDTO;
import com.supplymind.platform_core.dto.core.returns.RefundReturnDTO;
import com.supplymind.platform_core.model.core.ReturnReceipt;
import com.supplymind.platform_core.model.core.ReturnRequest;

public interface ReturnService {
    ReturnRequest createReturn(CreateReturnRequestDTO dto);

    ReturnRequest approveReturn(Long returnId, ApproveReturnDTO dto);

    ReturnReceipt receiveReturn(Long returnId, ReceiveReturnDTO dto);

    ReturnRequest refundReturn(Long returnId, RefundReturnDTO dto);

    ReturnRequest cancelReturn(Long returnId, String cancelledBy);

    ReturnRequest getReturn(Long returnId);
}
