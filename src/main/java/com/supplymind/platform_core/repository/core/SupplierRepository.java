package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Supplier> findByStripeConnectedAccountId(String stripeConnectedAccountId);

}

