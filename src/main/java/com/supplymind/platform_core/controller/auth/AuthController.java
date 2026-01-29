package com.supplymind.platform_core.controller.auth;
import com.supplymind.platform_core.config.JwtService;
import com.supplymind.platform_core.dto.auth.AuthResponse;
import com.supplymind.platform_core.dto.auth.LoginRequest;
import com.supplymind.platform_core.dto.auth.RegisterRequest;
import com.supplymind.platform_core.common.enums.Role;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));


        user.setRole(Role.STAFF);

        user.setIs2faEnabled(false);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully! Please ask a Manager to approve your account.");
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        if (user.getIs2faEnabled()) {
            //TODO: special response for 2FA
        }

        String jwtToken = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(jwtToken, "Login Successful"));
    }
}