package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.payments.CreatePaymentIntentRequestDTO;
import com.supplymind.platform_core.dto.core.payments.CreatePaymentIntentResponseDTO;
import com.supplymind.platform_core.dto.core.payments.CreateRefundRequestDTO;
import com.supplymind.platform_core.dto.core.payments.PaymentDTO;

public interface PaymentService {
    CreatePaymentIntentResponseDTO createPaymentIntent(CreatePaymentIntentRequestDTO dto);
    void requestRefund(CreateRefundRequestDTO dto); // webhook finalizes status

    PaymentDTO getPayment(Long paymentId);

}