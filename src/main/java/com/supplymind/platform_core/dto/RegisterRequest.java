package com.supplymind.platform_core.dto;

import com.supplymind.platform_core.model.auth.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.supplymind.platform_core.model.User}
 */

@Data
public class RegisterRequest implements Serializable {
    @NotNull
    @Size(max = 255)
    String email;
    @NotNull
    @Size(max = 255)
    String password;
    Role role;
}