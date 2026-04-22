package com.medsys.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "certificats_medicaux")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificatMedical {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String typeCertificat;
    private LocalDate dateEmission;
    private String medecinNomComplet;
    @Column(length = 2000) private String contenu;
    private Integer nombreJoursArret;
}
