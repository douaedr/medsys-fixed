package com.medsys.medicalrecord.controller;

import com.medsys.medicalrecord.entity.*;
import com.medsys.medicalrecord.service.DossierMedicalService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class DossierMedicalController {

    private final DossierMedicalService dossierService;

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','DIRECTEUR','PATIENT')")
    public ResponseEntity<DossierMedical> getDossier(@PathVariable Long patientId) {
        return ResponseEntity.ok(dossierService.getOrCreateDossier(patientId));
    }

    @PostMapping("/patient/{patientId}/consultations")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<DossierMedical> addConsultation(@PathVariable Long patientId,
                                                           @RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.addConsultation(patientId, req));
    }

    @PostMapping("/patient/{patientId}/antecedents")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<DossierMedical> addAntecedent(@PathVariable Long patientId,
                                                         @RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.addAntecedent(patientId, req));
    }

    @PostMapping("/patient/{patientId}/ordonnances")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<DossierMedical> addOrdonnance(@PathVariable Long patientId,
                                                         @RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.addOrdonnance(patientId, req));
    }

    @PostMapping("/patient/{patientId}/analyses")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<DossierMedical> addAnalyse(@PathVariable Long patientId,
                                                      @RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.addAnalyse(patientId, req));
    }

    @PostMapping("/patient/{patientId}/radiologies")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<DossierMedical> addRadiologie(@PathVariable Long patientId,
                                                         @RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.addRadiologie(patientId, req));
    }

    // ── Documents ──────────────────────────────────────────────────────────
    @PostMapping("/patient/{patientId}/documents")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT')")
    public ResponseEntity<DocumentPatient> uploadDocument(
            @PathVariable Long patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "AUTRE") String type,
            @RequestParam(value = "description", required = false) String description) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.uploadDocument(patientId, file, type, description));
    }

    @GetMapping("/patient/{patientId}/documents")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT','DIRECTEUR')")
    public ResponseEntity<List<DocumentPatient>> getDocuments(@PathVariable Long patientId) {
        return ResponseEntity.ok(dossierService.getDocuments(patientId));
    }

    @GetMapping("/documents/{documentId}/fichier")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) throws IOException {
        Path path = dossierService.getDocumentFile(documentId);
        Resource resource = new PathResource(path);
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT')")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long documentId) {
        dossierService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of("message", "Document supprimé"));
    }

    // ── Messagerie ─────────────────────────────────────────────────────────
    @PostMapping("/patient/{patientId}/messages")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT')")
    public ResponseEntity<MessagePatient> sendMessage(@PathVariable Long patientId,
                                                       @RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.sendMessage(
                patientId,
                String.valueOf(req.getOrDefault("expediteur", "PATIENT")),
                String.valueOf(req.getOrDefault("medecinId", "")),
                String.valueOf(req.getOrDefault("medecinNom", "")),
                String.valueOf(req.get("contenu"))
        ));
    }

    @GetMapping("/patient/{patientId}/messages")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT')")
    public ResponseEntity<List<MessagePatient>> getMessages(@PathVariable Long patientId) {
        return ResponseEntity.ok(dossierService.getMessages(patientId));
    }

    @PutMapping("/messages/{messageId}/lu")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN','PATIENT')")
    public ResponseEntity<MessagePatient> markAsRead(@PathVariable Long messageId) {
        return ResponseEntity.ok(dossierService.markAsRead(messageId));
    }
}
