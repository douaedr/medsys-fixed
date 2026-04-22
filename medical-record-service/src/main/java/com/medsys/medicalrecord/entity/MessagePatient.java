package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages_patient")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessagePatient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    private String expediteur;  // "PATIENT" ou "MEDECIN"
    private String medecinId;
    private String medecinNom;

    @Column(length = 2000, nullable = false)
    private String contenu;

    @Builder.Default
    private boolean lu = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime envoyeAt;
}
