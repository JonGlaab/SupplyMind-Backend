package com.supplymind.platform_core;

import com.supplymind.platform_core.service.intel.AiStatusScanner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AITest {

    @Autowired
    private AiStatusScanner aiScanner;

    @Test
    public void testAiConnection() {
        System.out.println("🤖 CONTACTING AI...");

        String sampleEmail = "We are sorry to inform you that the delivery for PO-102 is delayed. " +
                             "The new expected date is 2026-05-20.";

        AiStatusScanner.StatusScanResult result = aiScanner.scanEmailForStatus(sampleEmail);

        System.out.println("------------------------------------------------");
        System.out.println("✅ RAW STATUS: " + result.status());
        System.out.println("✅ DETECTED DATE: " + result.deliveryDate());
        System.out.println("------------------------------------------------");
    }
}