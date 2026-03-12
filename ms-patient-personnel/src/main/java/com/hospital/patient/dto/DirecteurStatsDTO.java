package com.hospital.patient.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirecteurStatsDTO {

    // Patients
    private Long totalPatients;
    private Long nouveauxCeMois;
    private Long masculins;
    private Long feminins;

    // Personnel médical
    private Long totalMedecins;

    // Dossiers & actes médicaux
    private Long totalDossiers;
    private Long totalConsultations;
    private Long totalOrdonnances;
    private Long totalAnalyses;
    private Long analysesEnAttente;
    private Long analysesEnCours;
    private Long analysesTerminees;
    private Long totalRadiologies;
    private Long totalHospitalisations;

    // Documents & messagerie
    private Long totalDocumentsUploades;
    private Long totalMessages;

    // Répartitions
    private List<Map<String, Object>> patientsParVille;
    private List<Map<String, Object>> patientsParGroupeSanguin;
    private List<Map<String, Object>> patientsParMois;
}
