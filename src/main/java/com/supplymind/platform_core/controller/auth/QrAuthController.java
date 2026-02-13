package com.supplymind.platform_core.controller.auth;

import com.supplymind.platform_core.service.auth.QrAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/qr")
public class QrAuthController {

    @Autowired private QrAuthService qrAuthService;

    @PostMapping("/approve")
    public ResponseEntity<?> approveLogin(
            @RequestParam String socketId,
            @AuthenticationPrincipal UserDetails mobileUser
    ) {
        if (mobileUser == null) {
            return ResponseEntity.status(401).body("Mobile device is not authenticated.");
        }

        try {
            qrAuthService.approveLogin(socketId, mobileUser.getUsername());

            return ResponseEntity.ok("Desktop Unlocked Successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to unlock desktop: " + e.getMessage());
        }
    }
}