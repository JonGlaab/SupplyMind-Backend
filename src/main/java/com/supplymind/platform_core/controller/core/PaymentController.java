package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.payments.*;
import com.supplymind.platform_core.service.core.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/core/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public CreatePaymentIntentResponseDTO createIntent(@RequestBody CreatePaymentIntentRequestDTO dto) {
        return paymentService.createPaymentIntent(dto);
    }

    @PostMapping("/refund")
    public void refund(@RequestBody CreateRefundRequestDTO dto) {
        paymentService.requestRefund(dto);
    }

    @GetMapping("/{paymentId}")
    public PaymentDTO getPayment(@PathVariable Long paymentId) {
        return paymentService.getPayment(paymentId);
    }

}
