package com.supplymind.platform_core.service.auth;

import com.supplymind.platform_core.config.JwtService;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap; // Import HashMap
import java.util.Map;

@Service
public class QrAuthService {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    public void approveLogin(String socketId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newDesktopToken = jwtService.generateToken(user);


        Map<String, String> payload = new HashMap<>();
        payload.put("token", newDesktopToken);
        payload.put("message", "Login Approved by Mobile");
        payload.put("role", user.getRole().name());
        payload.put("firstName", user.getFirstName());
        payload.put("lastName", user.getLastName());

        messagingTemplate.convertAndSend("/topic/login/" + socketId, payload);
    }
}