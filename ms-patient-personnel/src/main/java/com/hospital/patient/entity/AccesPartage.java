package com.hospital.patient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Accès temporaire partagé à un dossier médical.
 * Généré par un médecin, valide X heures, usage unique possible.
 */
@Entity
@Table(name = "acces_partages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccesPartage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierMedical dossierMedical;

    private String creePar;        // email du médecin créateur
    private String creePourNom;    // nom du destinataire (médecin externe, etc.)

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Builder.Default
    private boolean actif = true;

    private Integer maxUtilisations;   // null = illimité
    @Builder.Default
    private Integer utilisations = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpire() {
        return LocalDateTime.now().isAfter(expireAt);
    }

    public boolean isValide() {
        if (!actif || isExpire()) return false;
        if (maxUtilisations != null && utilisations >= maxUtilisations) return false;
        return true;
    }
}
