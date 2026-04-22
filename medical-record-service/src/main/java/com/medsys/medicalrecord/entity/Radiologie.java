package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "radiologies")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Radiologie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String typeExamen;
    private String radiologueNom;
    private LocalDate dateExamen;
    @Column(length = 500) private String description;
    @Column(length = 1000) private String conclusion;
    private String cheminFichier;
}
