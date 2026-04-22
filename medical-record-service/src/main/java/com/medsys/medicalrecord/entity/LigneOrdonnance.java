package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lignes_ordonnance")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneOrdonnance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String medicament;
    private String dosage;
    private String posologie;
    private Integer dureeJours;
    private String instructions;
}
