package com.hospital.patient.service;

import com.hospital.patient.dto.RendezVousDTO;
import com.hospital.patient.dto.RendezVousRequestDTO;
import com.hospital.patient.entity.Medecin;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.entity.RendezVous;
import com.hospital.patient.enums.StatutRdv;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.repository.MedecinRepository;
import com.hospital.patient.repository.PatientRepository;
import com.hospital.patient.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RendezVousService {

    private final RendezVousRepository rdvRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;

    @Transactional
    public RendezVousDTO createRdv(Long patientId, RendezVousRequestDTO req) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient non trouvé"));

        Medecin medecin = null;
        if (req.getMedecinId() != null) {
            medecin = medecinRepository.findById(req.getMedecinId()).orElse(null);
        }

        RendezVous rdv = RendezVous.builder()
                .patient(patient)
                .medecin(medecin)
                .dateHeure(req.getDateHeure())
                .motif(req.getMotif())
                .statut(StatutRdv.PLANIFIE)
                .service(req.getService())
                .lieu(req.getLieu())
                .notes(req.getNotes())
                .build();

        rdv = rdvRepository.save(rdv);
        log.info("RDV créé : patient={} date={}", patientId, req.getDateHeure());
        return toDTO(rdv);
    }

    @Transactional(readOnly = true)
    public List<RendezVousDTO> getRdvPatient(Long patientId) {
        return rdvRepository.findByPatientIdOrderByDateHeureDesc(patientId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RendezVousDTO> getAllRdv() {
        return rdvRepository.findAllByOrderByDateHeureDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public RendezVousDTO updateStatut(Long rdvId, Long patientId, StatutRdv nouveauStatut) {
        RendezVous rdv = rdvRepository.findById(rdvId)
                .orElseThrow(() -> new PatientNotFoundException("Rendez-vous non trouvé"));

        if (patientId != null && !rdv.getPatient().getId().equals(patientId)) {
            throw new PatientNotFoundException("Accès non autorisé à ce rendez-vous");
        }

        rdv.setStatut(nouveauStatut);
        return toDTO(rdvRepository.save(rdv));
    }

    @Transactional
    public RendezVousDTO annulerRdv(Long rdvId, Long patientId) {
        return updateStatut(rdvId, patientId, StatutRdv.ANNULE);
    }

    @Transactional
    public RendezVousDTO confirmerRdv(Long rdvId) {
        return updateStatut(rdvId, null, StatutRdv.CONFIRME);
    }

    @Transactional
    public RendezVousDTO completerRdv(Long rdvId) {
        return updateStatut(rdvId, null, StatutRdv.COMPLETE);
    }

    @Transactional
    public RendezVousDTO updateRdv(Long rdvId, RendezVousRequestDTO req) {
        RendezVous rdv = rdvRepository.findById(rdvId)
                .orElseThrow(() -> new PatientNotFoundException("Rendez-vous non trouvé"));

        if (req.getDateHeure() != null) rdv.setDateHeure(req.getDateHeure());
        if (req.getMotif() != null) rdv.setMotif(req.getMotif());
        if (req.getNotes() != null) rdv.setNotes(req.getNotes());
        if (req.getService() != null) rdv.setService(req.getService());
        if (req.getLieu() != null) rdv.setLieu(req.getLieu());
        if (req.getMedecinId() != null) {
            medecinRepository.findById(req.getMedecinId()).ifPresent(rdv::setMedecin);
        }

        return toDTO(rdvRepository.save(rdv));
    }

    @Transactional
    public void deleteRdv(Long rdvId) {
        if (!rdvRepository.existsById(rdvId)) {
            throw new PatientNotFoundException("Rendez-vous non trouvé");
        }
        rdvRepository.deleteById(rdvId);
    }

    public RendezVousDTO toDTO(RendezVous r) {
        return RendezVousDTO.builder()
                .id(r.getId())
                .date(r.getDateHeure() != null ? r.getDateHeure().toLocalDate().toString() : null)
                .heure(r.getDateHeure() != null ? r.getDateHeure().toLocalTime().toString() : null)
                .motif(r.getMotif())
                .statut(r.getStatut() != null ? r.getStatut().name() : null)
                .medecinNom(r.getMedecin() != null ? r.getMedecin().getNomComplet() : null)
                .medecinSpecialite(r.getMedecin() != null && r.getMedecin().getSpecialite() != null
                        ? r.getMedecin().getSpecialite().getNom() : null)
                .service(r.getService())
                .notes(r.getNotes())
                .build();
    }
}
