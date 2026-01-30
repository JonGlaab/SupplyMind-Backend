package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);
}

