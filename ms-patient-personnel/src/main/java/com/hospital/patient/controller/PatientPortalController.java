package com.hospital.patient.controller;

import com.hospital.patient.dto.DossierMedicalDTO;
import com.hospital.patient.dto.PatientResponseDTO;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.mapper.PatientMapper;
import com.hospital.patient.repository.PatientRepository;
import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
public class PatientPortalController {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final JwtService jwtService;
    private final PatientService patientService;

    /**
     * GET /api/v1/patient/me
     * Retourne les infos du patient connecté (via son token JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<PatientResponseDTO> getMyProfile(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long patientId = jwtService.extractPatientId(token);

        if (patientId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

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

        String token = authHeader.substring(7);
        Long patientId = jwtService.extractPatientId(token);

        if (patientId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(patientService.getDossierMedical(patientId));
    }
}
