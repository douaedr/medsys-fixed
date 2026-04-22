package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "hospitalisations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Hospitalisation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate dateEntree;
    private LocalDate dateSortie;
    private String service;
    private String medecinResponsable;
    @Column(length = 500) private String motif;
    @Column(length = 1000) private String compteRendu;
}
