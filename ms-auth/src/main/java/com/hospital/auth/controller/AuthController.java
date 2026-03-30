package com.hospital.auth.controller;

import com.hospital.auth.dto.*;
import com.hospital.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints de login, inscription et gestion des mots de passe")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne un token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie"),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect"),
        @ApiResponse(responseCode = "429", description = "Trop de tentatives - rate limit dépassé")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(summary = "Inscription patient", description = "Crée un compte patient avec dossier médical")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "409", description = "Email ou CIN déjà utilisé")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterPatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(req));
    }

    @Operation(summary = "Mot de passe oublié", description = "Envoie un email de réinitialisation")
    @ApiResponse(responseCode = "200", description = "Email envoyé si le compte existe")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé si le compte existe."));
    }

    @Operation(summary = "Réinitialiser le mot de passe", description = "Utilise le token reçu par email")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès."));
    }

    @Operation(summary = "Changer le mot de passe", description = "Nécessite d'être authentifié")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), req);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
    }

    @Operation(summary = "Vérifier un token JWT", description = "Retourne les informations du token")
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyToken(token));
    }

    @Operation(summary = "Profil utilisateur connecté")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("email", authentication.getName(), "authorities", authentication.getAuthorities()));
    }
}
