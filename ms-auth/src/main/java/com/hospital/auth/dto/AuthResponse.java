package com.hospital.auth.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private Long userId;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private Long patientId;
    private Long personnelId;
    // 2FA : si true, le token est absent et le client doit fournir le code email
    private Boolean requiresTwoFa;
    private String twoFaSessionId; // identifiant temporaire (email encodé) pour la 2e étape
}
