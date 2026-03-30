package com.hospital.patient.controller;

import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.OrdonnanceQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Ordonnances Numériques", description = "QR code de vérification des ordonnances")
public class OrdonnanceController {

    private final OrdonnanceQrService ordonnanceQrService;
    private final JwtService jwtService;

    // ─── Patient : QR code de son ordonnance ─────────────────────────────────

    @Operation(summary = "Générer le QR code d'une ordonnance")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/api/v1/patient/me/ordonnances/{id}/qrcode",
                produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrOrdonnance(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        String token = authHeader.substring(7);
        Long patientId = jwtService.extractPatientId(token);
        if (patientId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            byte[] qr = ordonnanceQrService.genererQrOrdonnance(id, patientId);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── Public : vérifier une ordonnance (pharmacien) ───────────────────────

    @Operation(
        summary = "Vérifier l'authenticité d'une ordonnance (public)",
        description = "Utilisé par le pharmacien via le QR code. Aucune authentification requise."
    )
    @GetMapping("/api/v1/public/ordonnances/verifier")
    public ResponseEntity<Map<String, Object>> verifierOrdonnance(
            @RequestParam Long ordonnanceId,
            @RequestParam String cin,
            @RequestParam String hash) {
        return ResponseEntity.ok(ordonnanceQrService.verifierOrdonnance(ordonnanceId, cin, hash));
    }
}
