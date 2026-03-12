package com.hospital.patient.controller;

import com.hospital.patient.dto.DocumentPatientDTO;
import com.hospital.patient.dto.DossierMedicalDTO;
import com.hospital.patient.dto.PatientResponseDTO;
import com.hospital.patient.entity.DocumentPatient;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.mapper.PatientMapper;
import com.hospital.patient.repository.PatientRepository;
import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.DocumentService;
import com.hospital.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
public class PatientPortalController {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final JwtService jwtService;
    private final PatientService patientService;
    private final DocumentService documentService;

    /**
     * GET /api/v1/patient/me
     * Retourne les infos du patient connecté (via son token JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<PatientResponseDTO> getMyProfile(
            @RequestHeader("Authorization") String authHeader) {

        Long patientId = extractPatientId(authHeader);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient non trouvé"));

        return ResponseEntity.ok(patientMapper.toResponseDTO(patient));
    }

    /**
     * GET /api/v1/patient/me/dossier
     * Retourne le dossier médical complet du patient connecté
     */
    @GetMapping("/me/dossier")
    public ResponseEntity<DossierMedicalDTO> getMyDossier(
            @RequestHeader("Authorization") String authHeader) {

        Long patientId = extractPatientId(authHeader);
        return ResponseEntity.ok(patientService.getDossierMedical(patientId));
    }

    // ─── Documents ────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/patient/me/documents
     * Upload un document (ordonnance, analyse, radio, etc.)
     */
    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam("type") String type,
            @RequestParam(value = "description", required = false, defaultValue = "") String description) {

        if (fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le fichier est vide"));
        }
        if (fichier.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "Fichier trop volumineux (max 10 MB)"));
        }

        try {
            Long patientId = extractPatientId(authHeader);
            DocumentPatientDTO dto = documentService.uploadDocument(patientId, fichier, type, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'enregistrement du fichier"));
        }
    }

    /**
     * GET /api/v1/patient/me/documents
     * Liste tous les documents du patient connecté
     */
    @GetMapping("/me/documents")
    public ResponseEntity<List<DocumentPatientDTO>> getMyDocuments(
            @RequestHeader("Authorization") String authHeader) {

        Long patientId = extractPatientId(authHeader);
        return ResponseEntity.ok(documentService.getDocuments(patientId));
    }

    /**
     * GET /api/v1/patient/me/documents/{id}/fichier
     * Télécharge / affiche un document
     */
    @GetMapping("/me/documents/{id}/fichier")
    public ResponseEntity<Resource> getDocumentFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        try {
            Long patientId = extractPatientId(authHeader);
            DocumentPatient doc = documentService.getDocumentForPatient(id, patientId);
            Resource resource = documentService.loadFileAsResource(doc.getCheminFichier());

            String contentType = doc.getContentType() != null ? doc.getContentType() : "application/octet-stream";
            String disposition = contentType.startsWith("image/") ? "inline" : "attachment";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename=\"" + doc.getNomFichierOriginal() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/v1/patient/me/documents/{id}
     * Supprime un document
     */
    @DeleteMapping("/me/documents/{id}")
    public ResponseEntity<?> deleteDocument(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        try {
            Long patientId = extractPatientId(authHeader);
            documentService.deleteDocument(id, patientId);
            return ResponseEntity.ok(Map.of("message", "Document supprimé avec succès"));
        } catch (PatientNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la suppression"));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long extractPatientId(String authHeader) {
        String token = authHeader.substring(7);
        Long patientId = jwtService.extractPatientId(token);
        if (patientId == null) {
            throw new PatientNotFoundException("Token invalide");
        }
        return patientId;
    }
}
