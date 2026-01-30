package com.supplymind.platform_core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
<<<<<<< Updated upstream
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
=======
>>>>>>> Stashed changes
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

<<<<<<< Updated upstream
import java.util.Arrays;
=======
>>>>>>> Stashed changes
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
<<<<<<< Updated upstream
                .csrf(csrf -> csrf.disable()) // Disable CSRF for external API calls
=======
                .csrf(csrf -> csrf.disable())
>>>>>>> Stashed changes
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/ping").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
<<<<<<< Updated upstream
                        .requestMatchers("/ws-auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
=======
                        .requestMatchers("/ws/**").permitAll()
>>>>>>> Stashed changes
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
<<<<<<< Updated upstream
        // Allow both local and production origins
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "https://supplymind-frontend-7c89888c6700.herokuapp.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
=======

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://localhost:5173",
                "http://192.168.*.*",
                "https://192.168.*.*",
                "https://*.ngrok-free.app",
                "https://*.ngrok-free.dev"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
>>>>>>> Stashed changes
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}