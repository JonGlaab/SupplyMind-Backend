package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.product.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse create(ProductCreateRequest req);
    Page<ProductResponse> list(Pageable pageable);
    ProductResponse get(Long productId);
    ProductResponse update(Long productId, ProductUpdateRequest req);
    void delete(Long productId);
}

