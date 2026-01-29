package com.supplymind.platform_core.model.auth;

import com.supplymind.platform_core.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_sessions", schema = "defaultdb", indexes = {
        @Index(name = "user_id", columnList = "user_id")
})
public class UserSession {
    @Id
    @Size(max = 255)
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @ColumnDefault("0")
    @Column(name = "mfa_verified")
    private Boolean mfaVerified;

    @NotNull
    @Column(name = "expiry_at", nullable = false)
    private Instant expiryAt;
}