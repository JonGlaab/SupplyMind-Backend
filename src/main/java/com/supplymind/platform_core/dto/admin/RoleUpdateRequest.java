package com.supplymind.platform_core.dto.admin;

import com.supplymind.platform_core.common.enums.Role;
import lombok.Data;

@Data
public class RoleUpdateRequest {
    private Role role;
}