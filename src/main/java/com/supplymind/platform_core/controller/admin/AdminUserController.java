package com.supplymind.platform_core.controller.admin;

import com.supplymind.platform_core.common.enums.Role;
import com.supplymind.platform_core.dto.auth.RegisterRequest;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        // In production, map this to a UserResponse DTO to hide password_hash
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email already exists!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.STAFF);
        user.setIs2faEnabled(false);

        userRepository.save(user);
        return ResponseEntity.ok("User created successfully by Admin.");
    }
}