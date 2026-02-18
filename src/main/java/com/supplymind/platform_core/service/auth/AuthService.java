package com.supplymind.platform_core.service.auth;

import com.supplymind.platform_core.config.JwtService;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import com.supplymind.platform_core.service.common.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StorageService storageService;

    public Map<String, Object> login(String email, String rawPassword) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtService.generateToken(user);

        return Map.of(
                "token", token,
                "role", user.getRole().name(),
                "needsPasswordChange", user.getNeedsPasswordChange()
        );
    }

    @Transactional
    public void changePassword(String email, String newPassword) {
        if ("123456".equals(newPassword)) {
            throw new IllegalArgumentException("Cannot use default password.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setNeedsPasswordChange(false);
        userRepository.save(user);
    }

    public Map<String, Object> getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());

        if (user.getSignatureUrl() != null && !user.getSignatureUrl().isBlank()) {
            // Assuming getSignatureUrl() stores the object key
            profile.put("signatureUrl", storageService.presignGetUrl(user.getSignatureUrl()));
        } else {
            profile.put("signatureUrl", "");
        }

        return profile;
    }

    public Optional<User> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByEmail(username);
    }

    @Transactional
    public String uploadSignature(String username, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old signature if it exists
        if (user.getSignatureUrl() != null && !user.getSignatureUrl().isBlank()) {
            storageService.deleteFile(user.getSignatureUrl());
        }

        File tempFile = File.createTempFile("signature-", file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        String objectKey = storageService.buildObjectKey("signature", user.getId(), file.getOriginalFilename());
        storageService.uploadFile(objectKey, tempFile, file.getContentType());

        user.setSignatureUrl(objectKey); // Save the KEY, not the public URL
        userRepository.save(user);

        tempFile.delete();

        return storageService.presignGetUrl(objectKey); // Return the temporary URL for immediate display
    }

    @Transactional
    public void removeSignature(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getSignatureUrl() != null && !user.getSignatureUrl().isBlank()) {
            storageService.deleteFile(user.getSignatureUrl());
        }

        user.setSignatureUrl(null);
        userRepository.save(user);
    }
}