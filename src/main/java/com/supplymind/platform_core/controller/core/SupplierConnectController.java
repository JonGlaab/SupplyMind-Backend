package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.finance.ConnectOnboardingLinkResponseDTO;
import com.supplymind.platform_core.dto.core.finance.ConnectStatusResponseDTO;
import com.supplymind.platform_core.model.core.Supplier;
import com.supplymind.platform_core.repository.core.SupplierRepository;
import com.supplymind.platform_core.service.impl.core.StripeConnectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/core/suppliers")
public class SupplierConnectController {

    private final StripeConnectService stripeConnectService;
    private final SupplierRepository supplierRepo;

    /**
     * Admin/Buyer clicks button -> gets onboarding link -> shares it for demo.
     */
    @PostMapping("/{supplierId}/connect/onboard")
    public ConnectOnboardingLinkResponseDTO onboard(@PathVariable Long supplierId) {

        // For demo: front-end will open these URLs in your app
        String refreshUrl = "http://localhost:5173/finance/connect/refresh?supplierId=" + supplierId;
        String returnUrl  = "http://localhost:5173/finance/connect/return?supplierId=" + supplierId;

        String url = stripeConnectService.generateOnboardingLink(supplierId, refreshUrl, returnUrl);

        Supplier s = supplierRepo.findById(supplierId).orElseThrow();
        return new ConnectOnboardingLinkResponseDTO(supplierId, s.getConnectStatus().name(), url);
    }

    /**
     * Refresh status (calls Stripe API and updates DB)
     */
    @GetMapping("/{supplierId}/connect/status")
    public ConnectStatusResponseDTO status(@PathVariable Long supplierId) {
        var st = stripeConnectService.refreshConnectStatusFromStripe(supplierId);
        Supplier s = supplierRepo.findById(supplierId).orElseThrow();
        return new ConnectStatusResponseDTO(supplierId, st.name(), s.getStripeConnectedAccountId());
    }

    /**
     * Demo-only shortcut (so demo never breaks)
     */
    @PostMapping("/{supplierId}/connect/mock-enable")
    public void mockEnable(@PathVariable Long supplierId) {
        stripeConnectService.mockEnable(supplierId);
    }
}

