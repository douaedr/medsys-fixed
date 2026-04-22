package com.medsys.medicalrecord.entity;

import com.medsys.medicalrecord.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents_patient")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentPatient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_medical_id")
    private DossierMedical dossierMedical;

    private String nomFichier;
    private String cheminFichier;
    private String typeContenu;

    @Enumerated(EnumType.STRING)
    private TypeDocument typeDocument;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}
