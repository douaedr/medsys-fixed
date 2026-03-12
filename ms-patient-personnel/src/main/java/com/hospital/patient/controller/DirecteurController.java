package com.hospital.patient.controller;

import com.hospital.patient.dto.*;
import com.hospital.patient.entity.Medecin;
import com.hospital.patient.repository.MedecinRepository;
import com.hospital.patient.service.DirecteurService;
import com.hospital.patient.service.PatientService;
import com.hospital.patient.service.PdfService;
import com.hospital.patient.service.RdvProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/directeur")
@RequiredArgsConstructor
public class DirecteurController {

    private final DirecteurService directeurService;
    private final PatientService patientService;
    private final MedecinRepository medecinRepository;
    private final PdfService pdfService;
    private final RdvProxyService rdvProxyService;

    /**
     * GET /api/v1/directeur/stats
     * Statistiques globales de l'établissement
     */
    @GetMapping("/stats")
    public ResponseEntity<DirecteurStatsDTO> getStats() {
        return ResponseEntity.ok(directeurService.getStats());
    }

    /**
     * GET /api/v1/directeur/patients
     * Liste paginée + recherche de tous les patients
     */
    @GetMapping("/patients")
    public ResponseEntity<Page<PatientResponseDTO>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "") String q) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        Page<PatientResponseDTO> result = q.isBlank()
                ? patientService.getAllPatients(pageable)
                : patientService.searchPatients(q, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/directeur/patients/{id}/dossier
     * Dossier médical complet d'un patient
     */
    @GetMapping("/patients/{id}/dossier")
    public ResponseEntity<DossierMedicalDTO> getDossier(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getDossierMedical(id));
    }

    /**
     * GET /api/v1/directeur/patients/{id}/dossier/pdf
     * Export PDF du dossier d'un patient
     */
    @GetMapping("/patients/{id}/dossier/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateDossierPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dossier-" + id + ".pdf\"")
                .body(pdf);
    }

    /**
     * GET /api/v1/directeur/medecins
     * Liste de tous les médecins
     */
    @GetMapping("/medecins")
    public ResponseEntity<List<Map<String, Object>>> getMedecins() {
        List<Map<String, Object>> medecins = medecinRepository.findAll().stream()
                .map(m -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", m.getId());
                    map.put("nom", m.getNom());
                    map.put("prenom", m.getPrenom());
                    map.put("nomComplet", m.getNomComplet());
                    map.put("matricule", m.getMatricule());
                    map.put("specialite", m.getSpecialite() != null ? m.getSpecialite().getNom() : null);
                    map.put("service", m.getService() != null ? m.getService().getNom() : null);
                    map.put("derniereSynchronisation", m.getDerniereSynchronisation());
                    return map;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(medecins);
    }

    /**
     * GET /api/v1/directeur/rdv
     * Tous les RDV en cours (proxy ms-rdv)
     */
    @GetMapping("/rdv")
    public ResponseEntity<List<RendezVousDTO>> getAllRdv(
            @RequestParam(required = false) Long patientId) {
        if (patientId != null) {
            return ResponseEntity.ok(rdvProxyService.getRdvPatient(patientId));
        }
        // Pour tous les RDV, appel de l'endpoint global ms-rdv si disponible
        return ResponseEntity.ok(rdvProxyService.getAllRdv());
    }
}
