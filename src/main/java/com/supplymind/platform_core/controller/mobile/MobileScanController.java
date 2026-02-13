package com.supplymind.platform_core.controller.mobile;

import com.supplymind.platform_core.dto.mobile.ScanAnalysisResponse;
import com.supplymind.platform_core.service.mobile.MobileScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/scan")
public class MobileScanController {

    @Autowired
    private MobileScanService scanService;

    /**
     * Entry point for the "Smart Scan" button on the mobile app.
     * Analyzes if a code is a PO, a Product SKU, or Ambiguous.
     */
    @GetMapping("/analyze")
    public ResponseEntity<ScanAnalysisResponse> analyzeScan(@RequestParam String code) {
        ScanAnalysisResponse response = scanService.analyzeCode(code);
        return ResponseEntity.ok(response);
    }
}