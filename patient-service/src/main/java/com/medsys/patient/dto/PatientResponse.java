package com.medsys.patient.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PatientResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String cin;
    private LocalDate dateNaissance;
    private String sexe;
    private String groupeSanguin;
    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    private String mutuelle;
    private String numeroCNSS;
    private LocalDateTime createdAt;
}
