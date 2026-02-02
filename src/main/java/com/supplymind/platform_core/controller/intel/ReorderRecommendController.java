package com.supplymind.platform_core.controller.intel;

import com.supplymind.platform_core.dto.intel.reorder.ReorderRecommendationRequest;
import com.supplymind.platform_core.dto.intel.reorder.ReorderRecommendationResponse;
import com.supplymind.platform_core.service.intel.ReorderRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/intel/reorder-recommend")
@RequiredArgsConstructor
public class ReorderRecommendController {

    private final ReorderRecommendService optimizationService;

    @PostMapping("/calculate")
    public ResponseEntity<ReorderRecommendationResponse> getRecommendation(
            @RequestBody ReorderRecommendationRequest request) {

        ReorderRecommendationResponse response = optimizationService.generateRecommendation(request);
        return ResponseEntity.ok(response);
    }
}