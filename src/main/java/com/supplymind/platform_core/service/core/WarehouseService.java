package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.warehouse.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {

    WarehouseResponse create(WarehouseCreateRequest req);

    Page<WarehouseResponse> list(Pageable pageable);

    WarehouseResponse get(Long warehouseId);

    WarehouseResponse update(Long warehouseId, WarehouseUpdateRequest req);
}
