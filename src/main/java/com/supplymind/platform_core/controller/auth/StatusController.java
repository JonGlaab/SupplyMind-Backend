package com.supplymind.platform_core.controller.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/")
    public String home() {
        return "🟢 SupplyMind Backend is Running!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}