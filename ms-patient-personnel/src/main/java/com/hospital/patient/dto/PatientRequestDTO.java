package com.hospital.patient.dto;

import com.hospital.patient.enums.GroupeSanguin;
import com.hospital.patient.enums.Sexe;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit avoir entre 2 et 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit avoir entre 2 et 100 caractères")
    private String prenom;

    @NotBlank(message = "Le CIN est obligatoire")
    @Size(min = 6, max = 20, message = "CIN invalide (6-20 caractères)")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "CIN ne doit contenir que des lettres et chiffres")
    private String cin;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    private Sexe sexe;
    private GroupeSanguin groupeSanguin;

    @Pattern(regexp = "^[+]?[0-9\\s\\-]{6,20}$", message = "Format téléphone invalide")
    private String telephone;

    @Email(message = "Format email invalide")
    @Size(max = 150)
    private String email;

    @Size(max = 255)
    private String adresse;
    @Size(max = 100)
    private String ville;
    @Size(max = 100)
    private String mutuelle;
    @Size(max = 50)
    private String numeroCNSS;

    // Optionnel : antécédents, ordonnances, analyses à créer avec le patient
    private List<AntecedentItem> antecedents;
    private List<OrdonnanceItem> ordonnances;
    private List<AnalyseItem> analyses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AntecedentItem {
        private String type;           // MEDICAL, CHIRURGICAL, FAMILIAL, ALLERGIE
        private String description;
        private LocalDate dateApparition;
        private Boolean actif = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdonnanceItem {
        private LocalDate date;
        private String type;           // TRAITEMENT_COURT, TRAITEMENT_LONG, RENOUVELLEMENT
        private String medicaments;    // texte libre des médicaments
        private String observations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyseItem {
        private String typeAnalyse;
        private LocalDate dateAnalyse;
        private String resultats;
        private String laboratoire;
        private String statut;         // EN_ATTENTE, TERMINE, EN_COURS
    }
}
