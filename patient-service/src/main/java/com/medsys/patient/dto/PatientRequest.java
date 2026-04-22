package com.medsys.patient.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRequest {
    @NotBlank private String nom;
    @NotBlank private String prenom;
    @NotBlank private String cin;
    @NotNull  private LocalDate dateNaissance;
    private String sexe;
    private String groupeSanguin;
    private String telephone;
    @Email private String email;
    private String adresse;
    private String ville;
    private String mutuelle;
    private String numeroCNSS;
}
