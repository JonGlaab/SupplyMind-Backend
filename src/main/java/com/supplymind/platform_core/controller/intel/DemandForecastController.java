package com.supplymind.platform_core.controller.intel;

import com.supplymind.platform_core.dto.intel.forecast.ForecastResponse;
import com.supplymind.platform_core.service.intel.DemandForecastingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intel/demand")
@RequiredArgsConstructor
public class DemandForecastController {

    private final DemandForecastingService forecastingService;

    @GetMapping("/{productId}")
    public ResponseEntity<ForecastResponse> getProductForecast(@PathVariable Long productId) {
        ForecastResponse response = forecastingService.calculateForecast(productId);
        return ResponseEntity.ok(response);
    }
}