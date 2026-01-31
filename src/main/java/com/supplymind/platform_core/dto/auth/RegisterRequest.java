package com.supplymind.platform_core.dto.auth;

import com.supplymind.platform_core.common.enums.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.supplymind.platform_core.model.auth.User}
 */

@Data
public class RegisterRequest implements Serializable {
    @NotNull
    @Size(max = 255)
    String email;
    @NotNull
    @Size(max = 255)
    String password;
    private String firstName;
    private String lastName;
    Role role;
}