package com.supplymind.platform_core.model;

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

}