package com.medsys.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medsys.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String cin;

    /** Référence vers patient-service (PATIENT uniquement) */
    private Long patientId;

    /** Référence vers les données personnelles (MEDECIN, SECRETAIRE, etc.) */
    private Long personnelId;

    @Builder.Default
    private boolean enabled = true;

    // ── Vérification email ─────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private boolean emailVerified = false;

    @JsonIgnore
    private String emailVerificationToken;

    // ── Réinitialisation mot de passe ──────────────────────────────────────
    @JsonIgnore
    private String resetToken;

    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    // ── Refresh token ──────────────────────────────────────────────────────
    @JsonIgnore
    private String refreshToken;

    @JsonIgnore
    private LocalDateTime refreshTokenExpiry;

    // ── Protection brute-force ─────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private LocalDateTime accountLockedUntil;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Helpers ────────────────────────────────────────────────────────────
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void lockAccount(int lockMinutes) {
        this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
    }
}
