package com.medsys.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class RegisterPatientRequest {
    @NotBlank private String nom;
    @NotBlank private String prenom;
    @NotBlank private String cin;
    @NotNull  private LocalDate dateNaissance;
    @NotBlank private String sexe;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 8) private String password;
    private String groupeSanguin;
    private String telephone;
    private String adresse;
    private String ville;
    private String mutuelle;
    private String numeroCNSS;
    private List<Map<String, Object>> antecedents;
    private List<Map<String, Object>> ordonnances;
    private List<Map<String, Object>> analyses;
}
