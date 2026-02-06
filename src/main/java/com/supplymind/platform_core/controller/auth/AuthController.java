package com.supplymind.platform_core.controller.auth;

import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
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

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        try {
            Map<String, Object> response = authService.login(
                    req.get("email"),
                    req.get("password")
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("error", "Invalid Credentials"));
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
    @PutMapping("/me/signature")
    public ResponseEntity<?> updateSignature(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        try {
            String newUrl = body.get("signatureUrl");

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setSignatureUrl(newUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Signature updated"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update signature");
        }
    }
}