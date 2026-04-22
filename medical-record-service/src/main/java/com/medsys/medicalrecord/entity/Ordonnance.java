package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordonnances")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Ordonnance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateOrdonnance;
    private String typeOrdonnance;
    private String medecinNomComplet;

    @Column(length = 500)
    private String instructions;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordonnance_id")
    @Builder.Default
    private List<LigneOrdonnance> lignes = new ArrayList<>();
}
