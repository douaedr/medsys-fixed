package com.hospital.patient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hospital.patient.dto.DossierMedicalDTO;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Export RGPD : génère un ZIP contenant toutes les données du patient.
 * - patient.json : informations personnelles
 * - dossier.json : dossier médical complet
 * - dossier.pdf  : export PDF du dossier
 * - README.txt   : explication du contenu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportRgpdService {

    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public byte[] exporterDonneesPatient(Long patientId) throws IOException {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient non trouvé"));

        DossierMedicalDTO dossier = patientService.getDossierMedical(patientId);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {

            // 1. README
            zip.putNextEntry(new ZipEntry("README.txt"));
            zip.write(genererReadme(patient).getBytes("UTF-8"));
            zip.closeEntry();

            // 2. Données personnelles JSON
            zip.putNextEntry(new ZipEntry("patient.json"));
            Map<String, Object> infoPerso = Map.of(
                    "id", patient.getId(),
                    "nom", patient.getNom(),
                    "prenom", patient.getPrenom(),
                    "cin", patient.getCin(),
                    "dateNaissance", patient.getDateNaissance().toString(),
                    "sexe", patient.getSexe() != null ? patient.getSexe().name() : null,
                    "email", patient.getEmail() != null ? patient.getEmail() : "",
                    "telephone", patient.getTelephone() != null ? patient.getTelephone() : "",
                    "adresse", patient.getAdresse() != null ? patient.getAdresse() : "",
                    "ville", patient.getVille() != null ? patient.getVille() : ""
            );
            zip.write(mapper.writeValueAsBytes(infoPerso));
            zip.closeEntry();

            // 3. Dossier médical JSON
            zip.putNextEntry(new ZipEntry("dossier_medical.json"));
            zip.write(mapper.writeValueAsBytes(dossier));
            zip.closeEntry();

            // 4. Export PDF
            try {
                byte[] pdf = pdfService.generateDossierPdf(patientId);
                zip.putNextEntry(new ZipEntry("dossier_medical.pdf"));
                zip.write(pdf);
                zip.closeEntry();
            } catch (Exception e) {
                log.warn("Impossible de générer le PDF pour l'export RGPD du patient {}: {}", patientId, e.getMessage());
            }
        }

        log.info("Export RGPD généré pour patient {}", patientId);
        return baos.toByteArray();
    }

    private String genererReadme(Patient patient) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return "EXPORT DE DONNÉES PERSONNELLES - MEDSYS\n" +
               "========================================\n\n" +
               "Conformément au Règlement Général sur la Protection des Données (RGPD),\n" +
               "voici l'ensemble des données vous concernant dans notre système.\n\n" +
               "Export généré le : " + date + "\n" +
               "Patient          : " + patient.getPrenom() + " " + patient.getNom() + "\n" +
               "CIN              : " + patient.getCin() + "\n\n" +
               "CONTENU DU ZIP :\n" +
               "  - patient.json         : Vos informations personnelles\n" +
               "  - dossier_medical.json : Votre dossier médical complet (JSON)\n" +
               "  - dossier_medical.pdf  : Votre dossier médical (PDF lisible)\n\n" +
               "Pour toute question concernant vos données, contactez :\n" +
               "  dpo@medsys-hospital.ma\n\n" +
               "MedSys - Système d'Information Hospitalier\n";
    }
}
