package com.supplymind.platform_core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@Profile("local")
public class DevUsersConfig {

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("proc").password(encoder.encode("proc123")).roles("PROCUREMENT_OFFICER").build(),
                User.withUsername("manager").password(encoder.encode("manager123")).roles("MANAGER").build(),
                User.withUsername("staff").password(encoder.encode("staff123")).roles("STAFF").build(),
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build()
        );
    }
}
