package com.supplymind.platform_core.controller.core;


import com.supplymind.platform_core.dto.core.returns.ApproveReturnDTO;
import com.supplymind.platform_core.dto.core.returns.CreateReturnRequestDTO;
import com.supplymind.platform_core.dto.core.returns.ReceiveReturnDTO;
import com.supplymind.platform_core.model.core.ReturnReceipt;
import com.supplymind.platform_core.model.core.ReturnRequest;
import com.supplymind.platform_core.service.core.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('MANAGER')")
    public Page<ReturnRequest> listAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return returnService.getAllReturns(pageable);
    }

    @PostMapping
    public ReturnRequest create(@RequestBody CreateReturnRequestDTO dto) {
        return returnService.createReturn(dto);
    }

    @PostMapping("/{returnId}/approve")
    public ReturnRequest approve(@PathVariable Long returnId, @RequestBody ApproveReturnDTO dto) {
        return returnService.approveReturn(returnId, dto);
    }

    @PostMapping("/{returnId}/receive")
    public ReturnReceipt receive(@PathVariable Long returnId, @RequestBody ReceiveReturnDTO dto) {
        return returnService.receiveReturn(returnId, dto);
    }

    @GetMapping("/{returnId}")
    public ReturnRequest get(@PathVariable Long returnId) {
        return returnService.getReturn(returnId);
    }
}
