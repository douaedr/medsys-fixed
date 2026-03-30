package com.hospital.patient.controller;

import com.hospital.patient.dto.RendezVousDTO;
import com.hospital.patient.dto.RendezVousRequestDTO;
import com.hospital.patient.enums.StatutRdv;
import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.RendezVousService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rendez-vous", description = "Gestion des rendez-vous médicaux")
@SecurityRequirement(name = "bearerAuth")
public class RendezVousController {

    private final RendezVousService rdvService;
    private final JwtService jwtService;

    // ─── Patient : prendre / consulter ses RDV ───────────────────────────────

    @Operation(summary = "Patient : lister ses rendez-vous")
    @GetMapping("/api/v1/patient/me/rdv")
    public ResponseEntity<List<RendezVousDTO>> getMyRdv(
            @RequestHeader("Authorization") String authHeader) {
        Long patientId = extractPatientId(authHeader);
        return ResponseEntity.ok(rdvService.getRdvPatient(patientId));
    }

    @Operation(summary = "Patient : prendre un rendez-vous")
    @PostMapping("/api/v1/patient/me/rdv")
    public ResponseEntity<RendezVousDTO> createRdv(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RendezVousRequestDTO req) {
        Long patientId = extractPatientId(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(rdvService.createRdv(patientId, req));
    }

    @Operation(summary = "Patient : annuler un rendez-vous")
    @PutMapping("/api/v1/patient/me/rdv/{id}/annuler")
    public ResponseEntity<RendezVousDTO> annulerRdv(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Long patientId = extractPatientId(authHeader);
        return ResponseEntity.ok(rdvService.annulerRdv(id, patientId));
    }

    // ─── Médecin/Personnel : gérer les RDV ───────────────────────────────────

    @Operation(summary = "Médecin : voir tous les RDV")
    @GetMapping("/api/v1/rdv")
    public ResponseEntity<List<RendezVousDTO>> getAllRdv() {
        return ResponseEntity.ok(rdvService.getAllRdv());
    }

    @Operation(summary = "Médecin : confirmer un RDV")
    @PutMapping("/api/v1/rdv/{id}/confirmer")
    public ResponseEntity<RendezVousDTO> confirmerRdv(@PathVariable Long id) {
        return ResponseEntity.ok(rdvService.confirmerRdv(id));
    }

    @Operation(summary = "Médecin : marquer un RDV comme complété")
    @PutMapping("/api/v1/rdv/{id}/completer")
    public ResponseEntity<RendezVousDTO> completerRdv(@PathVariable Long id) {
        return ResponseEntity.ok(rdvService.completerRdv(id));
    }

    @Operation(summary = "Médecin : modifier un RDV")
    @PutMapping("/api/v1/rdv/{id}")
    public ResponseEntity<RendezVousDTO> updateRdv(@PathVariable Long id,
                                                    @Valid @RequestBody RendezVousRequestDTO req) {
        return ResponseEntity.ok(rdvService.updateRdv(id, req));
    }

    @Operation(summary = "Admin/Directeur : supprimer un RDV")
    @DeleteMapping("/api/v1/rdv/{id}")
    public ResponseEntity<Void> deleteRdv(@PathVariable Long id) {
        rdvService.deleteRdv(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "RDV d'un patient spécifique (médecin/directeur)")
    @GetMapping("/api/v1/patients/{patientId}/rdv")
    public ResponseEntity<List<RendezVousDTO>> getRdvPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(rdvService.getRdvPatient(patientId));
    }

    private Long extractPatientId(String authHeader) {
        String token = authHeader.substring(7);
        Long patientId = jwtService.extractPatientId(token);
        if (patientId == null) throw new com.hospital.patient.exception.PatientNotFoundException("Token invalide");
        return patientId;
    }
}
