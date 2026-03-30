package com.hospital.patient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_date", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;       // email de l'utilisateur
    private String role;

    @Column(nullable = false)
    private String action;       // ex: CONSULTER_DOSSIER, UPLOAD_DOCUMENT, LOGIN

    private String ressource;    // ex: PATIENT, DOSSIER, DOCUMENT
    private Long ressourceId;

    @Column(length = 1000)
    private String details;      // informations supplémentaires

    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NiveauLog niveau = NiveauLog.INFO;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum NiveauLog { INFO, AVERTISSEMENT, CRITIQUE }
}
