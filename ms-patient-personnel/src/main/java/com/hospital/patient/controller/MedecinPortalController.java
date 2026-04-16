package com.hospital.patient.controller;

import com.hospital.patient.dto.EnvoyerMessageRequest;
import com.hospital.patient.dto.MessagePatientDTO;
import com.hospital.patient.entity.Medecin;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.repository.MedecinRepository;
import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/medecin")
@RequiredArgsConstructor
public class MedecinPortalController {

    private final MessageService messageService;
    private final MedecinRepository medecinRepository;
    private final JwtService jwtService;

    // ── Messagerie ─────────────────────────────────────────────────────────────

    @GetMapping("/patients/{patientId}/messages")
    public ResponseEntity<List<MessagePatientDTO>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long patientId) {
        return ResponseEntity.ok(messageService.getMessages(patientId));
    }

    @PostMapping("/patients/{patientId}/messages")
    public ResponseEntity<MessagePatientDTO> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long patientId,
            @Valid @RequestBody EnvoyerMessageRequest req) {
        Long medecinId = extractMedecinId(authHeader);
        String medecinNom = resolveMedecinNom(medecinId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.envoyerMessageMedecin(medecinId, patientId, req.getContenu(), medecinNom));
    }

    @PutMapping("/patients/{patientId}/messages/{messageId}/lu")
    public ResponseEntity<?> marquerLu(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long patientId,
            @PathVariable Long messageId) {
        messageService.marquerLu(patientId, messageId);
        return ResponseEntity.ok(Map.of("message", "Message marqué comme lu"));
    }

    // ── Utilitaires ────────────────────────────────────────────────────────────

    private Long extractMedecinId(String authHeader) {
        String token = authHeader.substring(7);
        Long id = jwtService.extractUserId(token);
        if (id == null) throw new PatientNotFoundException("Token médecin invalide");
        return id;
    }

    private String resolveMedecinNom(Long medecinId) {
        return medecinRepository.findById(medecinId)
                .map(Medecin::getNomComplet)
                .orElse("Dr. Personnel Médical");
    }
}
