package com.medsys.medicalrecord.entity;

import com.medsys.medicalrecord.enums.TypeAntecedent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "antecedents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Antecedent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TypeAntecedent typeAntecedent;

    @Column(length = 1000)
    private String description;

    private String severite;
    private LocalDate dateDiagnostic;
}
