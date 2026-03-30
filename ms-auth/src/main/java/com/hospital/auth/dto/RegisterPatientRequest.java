package com.hospital.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterPatientRequest {

    // ── Étape 1 : Compte ──────────────────────────────────────
    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Mot de passe obligatoire")
    @Size(min = 8, message = "Minimum 8 caractères")
    private String password;

    // ── Étape 2 : Infos personnelles ──────────────────────────
    @NotBlank(message = "Nom obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit avoir entre 2 et 100 caractères")
    private String nom;

    @NotBlank(message = "Prénom obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit avoir entre 2 et 100 caractères")
    private String prenom;

    @NotBlank(message = "CIN obligatoire")
    @Size(min = 6, max = 20, message = "CIN invalide (6-20 caractères)")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "CIN ne doit contenir que des lettres et chiffres")
    private String cin;

    @NotNull(message = "Date de naissance obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    private String sexe;           // MASCULIN / FEMININ
    @Pattern(regexp = "^[+]?[0-9\\s\\-]{6,20}$", message = "Format téléphone invalide")
    private String telephone;
    @Size(max = 255, message = "Adresse trop longue")
    private String adresse;
    @Size(max = 100, message = "Ville trop longue")
    private String ville;

    // ── Étape 3 : Infos médicales ─────────────────────────────
    private String groupeSanguin;  // A_POSITIF, O_NEGATIF...
    private String mutuelle;
    private String numeroCNSS;

    // ── Étape 4 : Antécédents médicaux (optionnel) ────────────
    private List<AntecedentItem> antecedents;

    // ── Étape 5 : Ordonnances antérieures (optionnel) ─────────
    private List<OrdonnanceItem> ordonnances;

    // ── Étape 6 : Analyses antérieures (optionnel) ────────────
    private List<AnalyseItem> analyses;

    // ── Inner classes ─────────────────────────────────────────
    @Data
    public static class AntecedentItem {
        private String type;        // MEDICAL, CHIRURGICAL, FAMILIAL, ALLERGIE
        private String description;
        private LocalDate dateApparition;
        private Boolean actif = true;
    }

    @Data
    public static class OrdonnanceItem {
        private LocalDate date;
        private String type;        // TRAITEMENT_COURT, TRAITEMENT_LONG, RENOUVELLEMENT
        private String medicaments; // liste des médicaments
        private String observations;
    }

    @Data
    public static class AnalyseItem {
        private String typeAnalyse;
        private LocalDate dateAnalyse;
        private String resultats;
        private String laboratoire;
        private String statut;      // EN_ATTENTE, TERMINE, etc.
    }
}
