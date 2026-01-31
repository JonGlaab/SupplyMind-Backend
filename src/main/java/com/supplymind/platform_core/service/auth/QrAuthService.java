package com.supplymind.platform_core.service;

import com.supplymind.platform_core.config.JwtService;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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


        messagingTemplate.convertAndSend("/topic/login/" + socketId, Map.of(
                "token", newDesktopToken,
                "message", "Login Approved by Mobile"
        ));
    }
}