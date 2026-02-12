package com.supplymind.platform_core.service.impl.core;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.supplymind.platform_core.common.enums.SupplierConnectStatus;
import com.supplymind.platform_core.model.core.Supplier;
import com.supplymind.platform_core.repository.core.SupplierRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeConnectService {

    private final SupplierRepository supplierRepo;

    /**
     * Creates (if needed) a Stripe Connected Account (Express) and returns onboarding link.
     * Supplier has no login; admin shares this link for demo.
     */
    @Transactional
    public String generateOnboardingLink(Long supplierId, String refreshUrl, String returnUrl) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        try {
            String accountId = supplier.getStripeConnectedAccountId();

            if (accountId == null || accountId.isBlank()) {
                AccountCreateParams.Builder accountParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("CA"); // adjust if you want

                // if supplier has email, pass it
                if (supplier.getContactEmail() != null && !supplier.getContactEmail().isBlank()) {
                    accountParams.setEmail(supplier.getContactEmail());
                }

                Account account = Account.create(accountParams.build());
                supplier.setStripeConnectedAccountId(account.getId());
                supplier.setConnectStatus(SupplierConnectStatus.PENDING);
                supplierRepo.save(supplier);

                accountId = account.getId();
            } else {
                // ensure status at least PENDING
                if (supplier.getConnectStatus() == SupplierConnectStatus.NOT_STARTED) {
                    supplier.setConnectStatus(SupplierConnectStatus.PENDING);
                    supplierRepo.save(supplier);
                }
            }

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .setRefreshUrl(refreshUrl) // where to send user if they quit
                    .setReturnUrl(returnUrl)   // where to send after completion
                    .build();

            AccountLink link = AccountLink.create(linkParams);
            return link.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException("Stripe Connect onboarding error: " + e.getMessage(), e);
        }
    }

    /**
     * Pull account status from Stripe and update DB.
     */
    @Transactional
    public SupplierConnectStatus refreshConnectStatusFromStripe(Long supplierId) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        if (supplier.getStripeConnectedAccountId() == null || supplier.getStripeConnectedAccountId().isBlank()) {
            supplier.setConnectStatus(SupplierConnectStatus.NOT_STARTED);
            supplierRepo.save(supplier);
            return supplier.getConnectStatus();
        }

        try {
            Account account = Account.retrieve(supplier.getStripeConnectedAccountId());

            boolean payoutsEnabled = Boolean.TRUE.equals(account.getPayoutsEnabled());
            boolean detailsSubmitted = Boolean.TRUE.equals(account.getDetailsSubmitted());

            if (payoutsEnabled && detailsSubmitted) {
                supplier.setConnectStatus(SupplierConnectStatus.ENABLED);
            } else {
                supplier.setConnectStatus(SupplierConnectStatus.PENDING);
            }

            supplierRepo.save(supplier);
            return supplier.getConnectStatus();

        } catch (StripeException e) {
            throw new RuntimeException("Stripe Connect status check error: " + e.getMessage(), e);
        }
    }

    /**
     * Demo-only shortcut: mark supplier as ENABLED without real onboarding.
     */
    @Transactional
    public void mockEnable(Long supplierId) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        if (supplier.getStripeConnectedAccountId() == null || supplier.getStripeConnectedAccountId().isBlank()) {
            // create an account id anyway (so UI has something)
            try {
                Account account = Account.create(AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("CA")
                        .build());
                supplier.setStripeConnectedAccountId(account.getId());
            } catch (StripeException e) {
                throw new RuntimeException("Stripe mock enable error: " + e.getMessage(), e);
            }
        }

        supplier.setConnectStatus(SupplierConnectStatus.ENABLED);
        supplierRepo.save(supplier);
    }
}

