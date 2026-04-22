package com.medsys.patient.service;

import com.medsys.patient.dto.*;
import com.medsys.patient.entity.Patient;
import com.medsys.patient.enums.GroupeSanguin;
import com.medsys.patient.enums.Sexe;
import com.medsys.patient.exception.PatientNotFoundException;
import com.medsys.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepo;

    public List<PatientResponse> findAll() {
        return patientRepo.findAll().stream().map(this::toResponse).toList();
    }

    public PatientResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public PatientResponse findByCin(String cin) {
        return patientRepo.findByCin(cin)
                .map(this::toResponse)
                .orElseThrow(() -> new PatientNotFoundException("Patient non trouvé: cin=" + cin));
    }

    public PatientResponse create(PatientRequest req) {
        if (patientRepo.existsByCin(req.getCin()))
            throw new IllegalArgumentException("Un patient avec ce CIN existe déjà");
        if (req.getEmail() != null && patientRepo.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Un patient avec cet email existe déjà");

        Patient patient = Patient.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .cin(req.getCin().toUpperCase())
                .dateNaissance(req.getDateNaissance())
                .sexe(parseSexe(req.getSexe()))
                .groupeSanguin(parseGroupeSanguin(req.getGroupeSanguin()))
                .telephone(req.getTelephone())
                .email(req.getEmail())
                .adresse(req.getAdresse())
                .ville(req.getVille())
                .mutuelle(req.getMutuelle())
                .numeroCNSS(req.getNumeroCNSS())
                .build();

        Patient saved = patientRepo.save(patient);
        log.info("[PATIENT] Créé: id={}, cin={}", saved.getId(), saved.getCin());
        return toResponse(saved);
    }

    public PatientResponse update(Long id, PatientRequest req) {
        Patient patient = findOrThrow(id);
        patient.setNom(req.getNom());
        patient.setPrenom(req.getPrenom());
        patient.setDateNaissance(req.getDateNaissance());
        patient.setSexe(parseSexe(req.getSexe()));
        patient.setGroupeSanguin(parseGroupeSanguin(req.getGroupeSanguin()));
        patient.setTelephone(req.getTelephone());
        patient.setEmail(req.getEmail());
        patient.setAdresse(req.getAdresse());
        patient.setVille(req.getVille());
        patient.setMutuelle(req.getMutuelle());
        patient.setNumeroCNSS(req.getNumeroCNSS());
        return toResponse(patientRepo.save(patient));
    }

    public void delete(Long id) {
        if (!patientRepo.existsById(id))
            throw new PatientNotFoundException(id);
        patientRepo.deleteById(id);
        log.info("[PATIENT] Supprimé: id={}", id);
    }

    public List<PatientResponse> search(String query) {
        return patientRepo.search(query).stream().map(this::toResponse).toList();
    }

    public StatsResponse getStats() {
        List<Patient> all = patientRepo.findAll();
        LocalDateTime debutMois = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

        return StatsResponse.builder()
                .totalPatients(all.size())
                .nouveauxCeMois(patientRepo.countByCreatedAtAfter(debutMois))
                .parSexe(all.stream()
                        .filter(p -> p.getSexe() != null)
                        .collect(Collectors.groupingBy(p -> p.getSexe().name(), Collectors.counting())))
                .parGroupeSanguin(all.stream()
                        .filter(p -> p.getGroupeSanguin() != null)
                        .collect(Collectors.groupingBy(p -> p.getGroupeSanguin().name(), Collectors.counting())))
                .parVille(all.stream()
                        .filter(p -> p.getVille() != null)
                        .collect(Collectors.groupingBy(Patient::getVille, Collectors.counting())))
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private Patient findOrThrow(Long id) {
        return patientRepo.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
    }

    private PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .nom(p.getNom())
                .prenom(p.getPrenom())
                .cin(p.getCin())
                .dateNaissance(p.getDateNaissance())
                .sexe(p.getSexe() != null ? p.getSexe().name() : null)
                .groupeSanguin(p.getGroupeSanguin() != null ? p.getGroupeSanguin().name() : null)
                .telephone(p.getTelephone())
                .email(p.getEmail())
                .adresse(p.getAdresse())
                .ville(p.getVille())
                .mutuelle(p.getMutuelle())
                .numeroCNSS(p.getNumeroCNSS())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private Sexe parseSexe(String s) {
        if (s == null) return null;
        try { return Sexe.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }

    private GroupeSanguin parseGroupeSanguin(String s) {
        if (s == null) return null;
        try { return GroupeSanguin.valueOf(s.toUpperCase().replace("+", "_PLUS").replace("-", "_MOINS")); }
        catch (Exception e) { return null; }
    }
}
