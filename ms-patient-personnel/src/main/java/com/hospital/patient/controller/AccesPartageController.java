package com.hospital.patient.controller;

import com.hospital.patient.dto.DossierMedicalDTO;
import com.hospital.patient.entity.AccesPartage;
import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.AccesPartageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Partage de dossier", description = "Accès temporaire aux dossiers médicaux")
public class AccesPartageController {

    private final AccesPartageService accesPartageService;
    private final JwtService jwtService;

    // ─── Médecin : créer un lien de partage ───────────────────────────────────

    @Operation(summary = "Créer un lien de partage temporaire", description = "Médecin uniquement")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/patients/{patientId}/partager")
    public ResponseEntity<Map<String, Object>> creerPartage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "24") int dureeHeures,
            @RequestParam(required = false) String pourNom,
            @RequestParam(required = false) Integer maxUtilisations) {

        String emailMedecin = extractEmail(authHeader);
        return ResponseEntity.ok(accesPartageService.creerAcces(
                patientId, emailMedecin, pourNom, dureeHeures, maxUtilisations));
    }

    @Operation(summary = "Lister mes partages actifs")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/mes-partages")
    public ResponseEntity<List<AccesPartage>> mesPartages(
            @RequestHeader("Authorization") String authHeader) {
        String email = extractEmail(authHeader);
        return ResponseEntity.ok(accesPartageService.getMesPartages(email));
    }

    @Operation(summary = "Révoquer un partage")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/v1/partages/{id}")
    public ResponseEntity<Void> revoquer(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        String email = extractEmail(authHeader);
        accesPartageService.revoquerAcces(id, email);
        return ResponseEntity.noContent().build();
    }

    // ─── Public : accéder au dossier via token ────────────────────────────────

    @Operation(summary = "Consulter un dossier partagé (lien public)", description = "Aucune authentification requise, le token est le secret")
    @GetMapping("/api/v1/public/dossier-partage/{token}")
    public ResponseEntity<DossierMedicalDTO> accederDossier(@PathVariable String token) {
        return ResponseEntity.ok(accesPartageService.accederParToken(token));
    }

    private String extractEmail(String authHeader) {
        String tok = authHeader.substring(7);
        return jwtService.extractEmail(tok);
    }
}
