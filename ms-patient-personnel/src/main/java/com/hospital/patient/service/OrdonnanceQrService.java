package com.hospital.patient.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.hospital.patient.entity.Ordonnance;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.repository.OrdonnanceRepository;
import com.hospital.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Service pour les ordonnances numériques avec QR code de vérification.
 * Le QR code contient un hash de l'ordonnance qui permet de la vérifier.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdonnanceQrService {

    private final OrdonnanceRepository ordonnanceRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public byte[] genererQrOrdonnance(Long ordonnanceId, Long patientId) throws WriterException, IOException {
        Ordonnance ord = ordonnanceRepository.findById(ordonnanceId)
                .orElseThrow(() -> new PatientNotFoundException("Ordonnance non trouvée"));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient non trouvé"));

        String hash = calculerHash(ord, patient);

        String contenu = "MEDSYS-ORD:" + ordonnanceId +
                "|CIN:" + patient.getCin() +
                "|DATE:" + (ord.getDateOrdonnance() != null ? ord.getDateOrdonnance().toString() : "") +
                "|HASH:" + hash;

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifierOrdonnance(Long ordonnanceId, String cinPatient, String hash) {
        Ordonnance ord = ordonnanceRepository.findById(ordonnanceId).orElse(null);
        if (ord == null) {
            return Map.of("valide", false, "message", "Ordonnance non trouvée");
        }

        Patient patient = patientRepository.findByCin(cinPatient).orElse(null);
        if (patient == null) {
            return Map.of("valide", false, "message", "Patient non trouvé");
        }

        String hashAttendu = calculerHash(ord, patient);
        if (!hashAttendu.equals(hash)) {
            return Map.of("valide", false, "message", "Signature invalide - ordonnance falsifiée");
        }

        boolean expiree = ord.getDateExpiration() != null &&
                ord.getDateExpiration().isBefore(java.time.LocalDate.now());

        return Map.of(
                "valide", true,
                "expiree", expiree,
                "ordonnanceId", ordonnanceId,
                "patient", patient.getPrenom() + " " + patient.getNom(),
                "date", ord.getDateOrdonnance() != null ? ord.getDateOrdonnance().toString() : "",
                "medecin", ord.getMedecin() != null ? ord.getMedecin().getNomComplet() : "inconnu",
                "message", expiree ? "Ordonnance authentique mais expirée" : "Ordonnance authentique et valide"
        );
    }

    private String calculerHash(Ordonnance ord, Patient patient) {
        try {
            String data = "ORD" + ord.getId() +
                    patient.getCin() +
                    (ord.getDateOrdonnance() != null ? ord.getDateOrdonnance().toString() : "") +
                    (ord.getMedecin() != null ? ord.getMedecin().getId() : "");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "error";
        }
    }
}
