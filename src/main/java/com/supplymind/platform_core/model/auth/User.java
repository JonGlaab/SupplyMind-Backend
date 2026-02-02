package com.supplymind.platform_core.model.auth;
import com.supplymind.platform_core.common.enums.Role;
import com.supplymind.platform_core.model.auth.TwoFactorAuth;
import com.supplymind.platform_core.model.auth.UserSession;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "email", columnNames = {"email"})
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Size(max = 100)
    @NotNull
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @Size(max = 255)
    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @ColumnDefault("0")
    @Column(name = "is_2fa_enabled")
    private Boolean is2faEnabled;

    @OneToMany(mappedBy = "buyer")
    private Set<PurchaseOrder> purchaseOrders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<TwoFactorAuth> twoFactorAuths = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserSession> userSessions = new LinkedHashSet<>();

    @ColumnDefault("0")
    @Column(name = "needs_password_change")
    private Boolean needsPasswordChange = false;

}