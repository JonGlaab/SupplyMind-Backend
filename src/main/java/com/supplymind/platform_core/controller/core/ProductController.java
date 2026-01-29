package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.product.*;
import com.supplymind.platform_core.service.core.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest req) {
        return service.create(req);
    }

    @GetMapping
    public Page<ProductResponse> list(@PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = "productId") Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable Long productId) {
        return service.get(productId);
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(@PathVariable Long productId,
                                  @Valid @RequestBody ProductUpdateRequest req) {
        return service.update(productId, req);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId) {
        service.delete(productId);
    }
}

