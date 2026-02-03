package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.dto.core.warehouse.*;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.core.Warehouse;
import com.supplymind.platform_core.repository.core.WarehouseRepository;
import com.supplymind.platform_core.service.core.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository repo;

    @Override
    public WarehouseResponse create(WarehouseCreateRequest req) {
        Warehouse w = Warehouse.builder()
                .locationName(req.locationName().trim())
                .address(req.address())
                .capacity(req.capacity())
                .build();

        return toResponse(repo.save(w));
    }

    @Override
    public Page<WarehouseResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(this::toResponse);
    }

    @Override
    public WarehouseResponse get(Long warehouseId) {
        Warehouse w = repo.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + warehouseId));
        return toResponse(w);
    }

    @Override
    @Transactional
    public WarehouseResponse update(Long warehouseId, WarehouseUpdateRequest req) {
        Warehouse w = repo.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + warehouseId));

        if (req.locationName() != null) {
            String name = req.locationName().trim();
            if (!name.isBlank()) {
                w.setLocationName(name);
            }
        }

        if (req.address() != null) {
            w.setAddress(req.address());
        }

        if (req.capacity() != null) {
            w.setCapacity(req.capacity());
        }

        // no need to call save() explicitly, but it’s fine if you prefer:
        // repo.save(w);

        return toResponse(w);
    }

    private WarehouseResponse toResponse(Warehouse w) {
        return new WarehouseResponse(
                w.getWarehouseId(),
                w.getLocationName(),
                w.getAddress(),
                w.getCapacity(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
