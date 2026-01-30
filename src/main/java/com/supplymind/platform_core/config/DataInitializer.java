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
            if (!userRepository.existsByEmail("admin@supplymind.com")) {
                User admin = new User();
                admin.setEmail("admin@supplymind.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123")); // Set a secure temp password
                admin.setRole(Role.ADMIN);
                admin.setIs2faEnabled(false);
                userRepository.save(admin);
                System.out.println(">>> Default Admin Created: admin@supplymind.com / admin123");
            }
        };
    }
}