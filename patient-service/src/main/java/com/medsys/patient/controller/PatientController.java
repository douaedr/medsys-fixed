package com.medsys.patient.controller;

import com.medsys.patient.dto.*;
import com.medsys.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','DIRECTEUR')")
    public ResponseEntity<List<PatientResponse>> getAll() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','DIRECTEUR','PATIENT')")
    public ResponseEntity<PatientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.findById(id));
    }

    @GetMapping("/cin/{cin}")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN')")
    public ResponseEntity<PatientResponse> getByCin(@PathVariable String cin) {
        return ResponseEntity.ok(patientService.findByCin(cin));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN')")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(req));
    }

    /** Endpoint public utilisé par auth-service lors de l'inscription patient */
    @PostMapping("/register")
    public ResponseEntity<PatientResponse> registerFromAuth(@Valid @RequestBody PatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','PATIENT')")
    public ResponseEntity<PatientResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody PatientRequest req) {
        return ResponseEntity.ok(patientService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Patient supprimé"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','DIRECTEUR')")
    public ResponseEntity<List<PatientResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(patientService.search(q));
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(patientService.getStats());
    }
}
