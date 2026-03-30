package com.hospital.patient.service;

import com.hospital.patient.dto.DossierMedicalDTO;
import com.hospital.patient.entity.AccesPartage;
import com.hospital.patient.entity.DossierMedical;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.repository.AccesPartageRepository;
import com.hospital.patient.repository.DossierMedicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccesPartageService {

    private final AccesPartageRepository accesPartageRepository;
    private final DossierMedicalRepository dossierRepository;
    private final PatientService patientService;

    @Transactional
    public Map<String, Object> creerAcces(Long patientId, String creePar, String creePourNom,
                                           int dureeHeures, Integer maxUtilisations) {
        DossierMedical dossier = dossierRepository.findByPatient_Id(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Dossier non trouvé pour le patient " + patientId));

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

        AccesPartage acces = AccesPartage.builder()
                .token(token)
                .dossierMedical(dossier)
                .creePar(creePar)
                .creePourNom(creePourNom)
                .expireAt(LocalDateTime.now().plusHours(dureeHeures))
                .maxUtilisations(maxUtilisations)
                .build();

        accesPartageRepository.save(acces);
        log.info("Accès partagé créé par {} pour patient {} valide {}h", creePar, patientId, dureeHeures);

        return Map.of(
                "token", token,
                "expireAt", acces.getExpireAt().toString(),
                "lienAcces", "/api/v1/public/dossier-partage/" + token
        );
    }

    @Transactional
    public DossierMedicalDTO accederParToken(String token) {
        AccesPartage acces = accesPartageRepository.findByToken(token)
                .orElseThrow(() -> new PatientNotFoundException("Lien de partage invalide ou expiré"));

        if (!acces.isValide()) {
            throw new PatientNotFoundException("Ce lien de partage est expiré ou a atteint son nombre max d'utilisations");
        }

        acces.setUtilisations(acces.getUtilisations() + 1);
        accesPartageRepository.save(acces);

        Long patientId = acces.getDossierMedical().getPatient().getId();
        log.info("Dossier patient {} consulté via partage (token: {}...)", patientId, token.substring(0, 8));

        return patientService.getDossierMedical(patientId);
    }

    @Transactional
    public void revoquerAcces(Long accesId, String emailMedecin) {
        AccesPartage acces = accesPartageRepository.findById(accesId)
                .orElseThrow(() -> new PatientNotFoundException("Accès partagé non trouvé"));

        if (!acces.getCreePar().equals(emailMedecin)) {
            throw new IllegalArgumentException("Vous ne pouvez révoquer que vos propres partages");
        }

        acces.setActif(false);
        accesPartageRepository.save(acces);
    }

    @Transactional(readOnly = true)
    public List<AccesPartage> getMesPartages(String emailMedecin) {
        return accesPartageRepository.findByCreePar(emailMedecin);
    }
}
