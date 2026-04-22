package com.medsys.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private Long patientId;
    private Long personnelId;
    private boolean emailVerified;
}
