package com.supplymind.platform_core.controller.admin;

import com.supplymind.platform_core.common.enums.Role;
import com.supplymind.platform_core.dto.admin.RoleUpdateRequest;
import com.supplymind.platform_core.dto.admin.UserResponse;
import com.supplymind.platform_core.dto.auth.RegisterRequest;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getIs2faEnabled()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email already exists!");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.STAFF);
        user.setIs2faEnabled(false);
        user.setNeedsPasswordChange(true);

        userRepository.save(user);
        return ResponseEntity.ok("User created successfully by Admin.");
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<?> updateRole(
            @PathVariable Long userId,
            @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal String currentUserEmail) { // Get current admin's email

        return userRepository.findById(userId)
                .map(user -> {
                    // Safety Check: Prevent admin from demoting themselves
                    if (user.getEmail().equals(currentUserEmail) && request.getRole() != Role.ADMIN) {
                        return ResponseEntity.badRequest().body("Error: You cannot demote yourself from the Admin role.");
                    }

                    user.setRole(request.getRole());
                    userRepository.save(user);
                    return ResponseEntity.ok("User role updated to " + request.getRole());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal String currentUserEmail) { // Get email of the person making the request

        return userRepository.findById(userId)
                .map(user -> {
                    // Safety Guard: Cannot delete yourself
                    if (user.getEmail().equals(currentUserEmail)) {
                        return ResponseEntity.badRequest().body("Error: You cannot delete your own account.");
                    }

                    userRepository.delete(user);
                    return ResponseEntity.ok("User deleted successfully.");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long userId,
            @AuthenticationPrincipal String currentUserEmail) { // Capture active admin email

        return userRepository.findById(userId)
                .map(user -> {
                    // Safety Guard: Cannot reset your own password here
                    if (user.getEmail().equals(currentUserEmail)) {
                        return ResponseEntity.badRequest().body("Security Restriction: Use 'Settings' to change your own password.");
                    }

                    user.setPasswordHash(passwordEncoder.encode("123456"));
                    user.setNeedsPasswordChange(true);
                    userRepository.save(user);
                    return ResponseEntity.ok("Password reset successful.");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}