package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.supplier.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    SupplierResponse create(SupplierCreateRequest req);

    Page<SupplierResponse> list(Pageable pageable);

    SupplierResponse get(Long supplierId);

    SupplierResponse update(Long supplierId, SupplierUpdateRequest req);

    void delete(Long supplierId);
}

