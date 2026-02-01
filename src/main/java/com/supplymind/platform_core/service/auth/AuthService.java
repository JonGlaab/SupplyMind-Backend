package com.supplymind.platform_core.service.auth;

import com.supplymind.platform_core.config.JwtService;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    public Map<String, Object> login(String email, String rawPassword) {

        //
        try {
            System.out.println("--- DIAGNOSTIC START ---");
            // We use a raw query to see if the row exists at all
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                System.out.println("RESULT: No user found with email: " + email);
            } else {
                System.out.println("RESULT: User found! ID: " + user.getId() + ", Role: " + user.getRole());
            }
        } catch (Exception e) {
            System.out.println("!!! MAPPING ERROR DETECTED !!!");
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("--- DIAGNOSTIC END ---");
        //
        System.out.println("Total Users in DB: " + userRepository.count());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtService.generateToken(user);
        boolean needsChange = "123456".equals(rawPassword);

        return Map.of(
                "token", token,
                "role", user.getRole().name(),
                "needsPasswordChange", needsChange
        );
    }

    public void changePassword(String email, String newPassword) {
        if ("123456".equals(newPassword)) {
            throw new IllegalArgumentException("Cannot use default password.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    public Map<String, Object> getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Map.of(
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        );
    }
}