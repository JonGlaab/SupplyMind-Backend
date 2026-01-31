package com.supplymind.platform_core.controller.auth;

import com.supplymind.platform_core.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        try {
            Map<String, Object> response = authService.login(
                    req.get("email"),
                    req.get("password")
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Credentials");
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            authService.changePassword(
                    userDetails.getUsername(),
                    body.get("newPassword")
            );
            return ResponseEntity.ok("Password updated successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Invalid Token");
        }

        try {
            Map<String, Object> profile = authService.getUserProfile(userDetails.getUsername());
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}