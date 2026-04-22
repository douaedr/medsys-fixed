package com.medsys.medicalrecord.entity;

import com.medsys.medicalrecord.enums.StatutAnalyse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "analyses_laboratoire")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyseLaboratoire {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String typeAnalyse;
    private String laboratoire;
    private LocalDate dateAnalyse;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutAnalyse statut = StatutAnalyse.EN_ATTENTE;

    @Column(length = 2000)
    private String resultats;

    @Column(length = 500)
    private String observations;
}
