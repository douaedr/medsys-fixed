package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateConsultation;
    private String medecinId;
    private String medecinNomComplet;
    private String specialite;

    @Column(length = 500)
    private String motif;

    @Column(length = 2000)
    private String diagnostic;

    @Column(length = 2000)
    private String traitement;

    @Column(length = 1000)
    private String notes;

    // Constantes vitales
    private Double poids;
    private Double taille;
    private Double temperature;
    private Integer tensionSystolique;
    private Integer tensionDiastolique;
    private Integer frequenceCardiaque;
    private Integer frequenceRespiratoire;
    private Double imc;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
