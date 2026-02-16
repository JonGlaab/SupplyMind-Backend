package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.supplierproduct.*;

import java.util.List;

public interface SupplierProductService {

    SupplierProductResponse create(SupplierProductCreateRequest req);

    List<SupplierProductResponse> listBySupplier(Long supplierId);

    SupplierProductResponse update(Long id, SupplierProductUpdateRequest req);

    void delete(Long id);
}