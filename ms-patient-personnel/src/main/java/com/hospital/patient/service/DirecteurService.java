package com.hospital.patient.service;

import com.hospital.patient.dto.DirecteurStatsDTO;
import com.hospital.patient.entity.DossierMedical;
import com.hospital.patient.enums.StatutAnalyse;
import com.hospital.patient.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirecteurService {

    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final DossierMedicalRepository dossierRepository;
    private final AnalyseRepository analyseRepository;
    private final DocumentPatientRepository documentRepository;
    private final MessagePatientRepository messageRepository;

    @Transactional(readOnly = true)
    public DirecteurStatsDTO getStats() {
        LocalDateTime debutMois = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime sixMoisAvant = LocalDateTime.now().minusMonths(6);

        // Patients
        long totalPatients = patientRepository.count();
        long nouveauxCeMois = patientRepository.countPatientsCreatedAfter(debutMois);
        long masculins = patientRepository.findBySexe(com.hospital.patient.enums.Sexe.MASCULIN).size();
        long feminins = patientRepository.findBySexe(com.hospital.patient.enums.Sexe.FEMININ).size();

        // Médecins
        long totalMedecins = medecinRepository.count();

        // Dossiers
        List<DossierMedical> dossiers = dossierRepository.findAll();
        long totalDossiers = dossiers.size();
        long totalConsultations = dossiers.stream().mapToLong(d -> d.getConsultations().size()).sum();
        long totalOrdonnances = dossiers.stream().mapToLong(d -> d.getOrdonnances().size()).sum();
        long totalRadiologies = dossiers.stream().mapToLong(d -> d.getRadiologies().size()).sum();
        long totalHospitalisations = dossiers.stream().mapToLong(d -> d.getHospitalisations().size()).sum();

        // Analyses
        long totalAnalyses = analyseRepository.count();
        long analysesEnAttente = analyseRepository.countByStatut(StatutAnalyse.EN_ATTENTE);
        long analysesEnCours = analyseRepository.countByStatut(StatutAnalyse.EN_COURS);
        long analysesTerminees = analyseRepository.countByStatut(StatutAnalyse.TERMINE);

        // Documents & messages
        long totalDocuments = documentRepository.count();
        long totalMessages = messageRepository.count();

        // Répartition par ville (top 8)
        List<Map<String, Object>> parVille = patientRepository.countByVille()
                .stream().limit(8)
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("ville", row[0] != null ? row[0].toString() : "Inconnue");
                    m.put("count", ((Number) row[1]).longValue());
                    return m;
                }).collect(Collectors.toList());

        // Répartition par groupe sanguin
        List<Map<String, Object>> parGroupeSanguin = patientRepository.countByGroupeSanguin()
                .stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("groupe", row[0] != null ? row[0].toString().replace("_", " ") : "—");
                    m.put("count", ((Number) row[1]).longValue());
                    return m;
                }).collect(Collectors.toList());

        // Patients par mois (6 derniers mois)
        List<Map<String, Object>> parMois = patientRepository.countByMonth(sixMoisAvant)
                .stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    long count = ((Number) row[2]).longValue();
                    m.put("mois", String.format("%02d/%d", month, year));
                    m.put("count", count);
                    return m;
                }).collect(Collectors.toList());

        return DirecteurStatsDTO.builder()
                .totalPatients(totalPatients)
                .nouveauxCeMois(nouveauxCeMois)
                .masculins(masculins)
                .feminins(feminins)
                .totalMedecins(totalMedecins)
                .totalDossiers(totalDossiers)
                .totalConsultations(totalConsultations)
                .totalOrdonnances(totalOrdonnances)
                .totalAnalyses(totalAnalyses)
                .analysesEnAttente(analysesEnAttente)
                .analysesEnCours(analysesEnCours)
                .analysesTerminees(analysesTerminees)
                .totalRadiologies(totalRadiologies)
                .totalHospitalisations(totalHospitalisations)
                .totalDocumentsUploades(totalDocuments)
                .totalMessages(totalMessages)
                .patientsParVille(parVille)
                .patientsParGroupeSanguin(parGroupeSanguin)
                .patientsParMois(parMois)
                .build();
    }
}
