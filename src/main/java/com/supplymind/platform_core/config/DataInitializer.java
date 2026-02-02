package com.supplymind.platform_core.config;

import com.supplymind.platform_core.common.enums.Role;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 1. ADMIN (admin123)
            createUserIfNotFound(
                    "supplymind.demo.2025@gmail.com",
                    "admin123",
                    "System", "Admin",
                    Role.ADMIN
            );

            // 2. MANAGER (manager123)
            createUserIfNotFound(
                    "supplymind.demo.2025+manager@gmail.com",
                    "manager123",
                    "Alice", "Manager",
                    Role.MANAGER
            );

            // 3. STAFF (staff123)
            createUserIfNotFound(
                    "supplymind.demo.2025+staff@gmail.com",
                    "staff123",
                    "Bob", "Staff",
                    Role.STAFF
            );
        };
    }

    private void createUserIfNotFound(String email, String rawPassword, String first, String last, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setFirstName(first);
            user.setLastName(last);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setIs2faEnabled(false);

            userRepository.save(user);
            System.out.println(">>> Seeded User: " + email + " / " + rawPassword);
        }
    }
}