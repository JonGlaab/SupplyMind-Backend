package com.supplymind.platform_core.dto.auth;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}