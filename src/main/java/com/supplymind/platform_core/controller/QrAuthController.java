package com.supplymind.platform_core.controller;

import com.supplymind.platform_core.config.JwtService;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/qr")
public class QrAuthController {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    // The MOBILE APP calls this endpoint to unlock the desktop
    @PostMapping("/approve")
    public ResponseEntity<?> approveLogin(
            @RequestParam String socketId,
            @AuthenticationPrincipal UserDetails mobileUser
    ) {
        if (mobileUser == null) {
            return ResponseEntity.status(401).body("Mobile device is not authenticated.");
        }


        User user = userRepository.findByEmail(mobileUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        String newDesktopToken = jwtService.generateToken(user);


        messagingTemplate.convertAndSend("/topic/login/" + socketId,(Object) Map.of(
                "token", newDesktopToken,
                "message", "Login Approved by Mobile"
        ));

        return ResponseEntity.ok("Desktop Unlocked Successfully");
    }
}