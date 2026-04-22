package com.medsys.medicalrecord.service;

import com.medsys.medicalrecord.entity.*;
import com.medsys.medicalrecord.enums.StatutAnalyse;
import com.medsys.medicalrecord.enums.TypeAntecedent;
import com.medsys.medicalrecord.enums.TypeDocument;
import com.medsys.medicalrecord.repository.DossierMedicalRepository;
import com.medsys.medicalrecord.repository.DocumentPatientRepository;
import com.medsys.medicalrecord.repository.MessagePatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DossierMedicalService {

    private final DossierMedicalRepository dossierRepo;
    private final DocumentPatientRepository documentRepo;
    private final MessagePatientRepository messageRepo;

    @Value("${app.upload-dir:uploads/medical-records}")
    private String uploadDir;

    // ── Dossier médical ────────────────────────────────────────────────────
    public DossierMedical getOrCreateDossier(Long patientId) {
        return dossierRepo.findByPatientId(patientId).orElseGet(() -> {
            DossierMedical dossier = DossierMedical.builder()
                    .patientId(patientId)
                    .build();
            DossierMedical saved = dossierRepo.save(dossier);
            log.info("[DOSSIER] Créé pour patientId={}, numéro={}", patientId, saved.getNumeroDossier());
            return saved;
        });
    }

    public DossierMedical getByPatientId(Long patientId) {
        return dossierRepo.findByPatientId(patientId)
                .orElseThrow(() -> new NoSuchElementException("Dossier non trouvé pour patientId=" + patientId));
    }

    // ── Consultations ──────────────────────────────────────────────────────
    public DossierMedical addConsultation(Long patientId, Map<String, Object> req) {
        DossierMedical dossier = getOrCreateDossier(patientId);
        Consultation c = Consultation.builder()
                .dateConsultation(parseDate(req, "dateConsultation"))
                .medecinId(str(req, "medecinId"))
                .medecinNomComplet(str(req, "medecinNomComplet"))
                .specialite(str(req, "specialite"))
                .motif(str(req, "motif"))
                .diagnostic(str(req, "diagnostic"))
                .traitement(str(req, "traitement"))
                .notes(str(req, "notes"))
                .poids(dbl(req, "poids"))
                .taille(dbl(req, "taille"))
                .temperature(dbl(req, "temperature"))
                .tensionSystolique(intVal(req, "tensionSystolique"))
                .tensionDiastolique(intVal(req, "tensionDiastolique"))
                .frequenceCardiaque(intVal(req, "frequenceCardiaque"))
                .build();
        dossier.getConsultations().add(c);
        return dossierRepo.save(dossier);
    }

    // ── Antécédents ────────────────────────────────────────────────────────
    public DossierMedical addAntecedent(Long patientId, Map<String, Object> req) {
        DossierMedical dossier = getOrCreateDossier(patientId);
        Antecedent a = Antecedent.builder()
                .typeAntecedent(parseEnum(TypeAntecedent.class, str(req, "typeAntecedent")))
                .description(str(req, "description"))
                .severite(str(req, "severite"))
                .dateDiagnostic(parseDate(req, "dateDiagnostic"))
                .build();
        dossier.getAntecedents().add(a);
        return dossierRepo.save(dossier);
    }

    // ── Ordonnances ────────────────────────────────────────────────────────
    public DossierMedical addOrdonnance(Long patientId, Map<String, Object> req) {
        DossierMedical dossier = getOrCreateDossier(patientId);
        Ordonnance o = Ordonnance.builder()
                .dateOrdonnance(parseDate(req, "dateOrdonnance"))
                .typeOrdonnance(str(req, "typeOrdonnance"))
                .medecinNomComplet(str(req, "medecinNomComplet"))
                .instructions(str(req, "instructions"))
                .build();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lignes = (List<Map<String, Object>>) req.getOrDefault("lignes", List.of());
        for (Map<String, Object> l : lignes) {
            o.getLignes().add(LigneOrdonnance.builder()
                    .medicament(str(l, "medicament"))
                    .dosage(str(l, "dosage"))
                    .posologie(str(l, "posologie"))
                    .dureeJours(intVal(l, "dureeJours"))
                    .instructions(str(l, "instructions"))
                    .build());
        }
        dossier.getOrdonnances().add(o);
        return dossierRepo.save(dossier);
    }

    // ── Analyses ───────────────────────────────────────────────────────────
    public DossierMedical addAnalyse(Long patientId, Map<String, Object> req) {
        DossierMedical dossier = getOrCreateDossier(patientId);
        AnalyseLaboratoire a = AnalyseLaboratoire.builder()
                .typeAnalyse(str(req, "typeAnalyse"))
                .laboratoire(str(req, "laboratoire"))
                .dateAnalyse(parseDate(req, "dateAnalyse"))
                .statut(parseEnum(StatutAnalyse.class, str(req, "statut"), StatutAnalyse.EN_ATTENTE))
                .resultats(str(req, "resultats"))
                .observations(str(req, "observations"))
                .build();
        dossier.getAnalyses().add(a);
        return dossierRepo.save(dossier);
    }

    // ── Radiologies ────────────────────────────────────────────────────────
    public DossierMedical addRadiologie(Long patientId, Map<String, Object> req) {
        DossierMedical dossier = getOrCreateDossier(patientId);
        Radiologie r = Radiologie.builder()
                .typeExamen(str(req, "typeExamen"))
                .radiologueNom(str(req, "radiologueNom"))
                .dateExamen(parseDate(req, "dateExamen"))
                .description(str(req, "description"))
                .conclusion(str(req, "conclusion"))
                .build();
        dossier.getRadiologies().add(r);
        return dossierRepo.save(dossier);
    }

    // ── Documents ──────────────────────────────────────────────────────────
    public DocumentPatient uploadDocument(Long patientId, MultipartFile file,
                                          String typeDoc, String description) throws IOException {
        validateFile(file);
        DossierMedical dossier = getOrCreateDossier(patientId);

        String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String filename = UUID.randomUUID().toString() + ext;
        Path dir = Paths.get(uploadDir, String.valueOf(patientId));
        Files.createDirectories(dir);

        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir.normalize()))
            throw new IllegalArgumentException("Chemin de fichier invalide");

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        DocumentPatient doc = DocumentPatient.builder()
                .dossierMedical(dossier)
                .nomFichier(file.getOriginalFilename())
                .cheminFichier(target.toString())
                .typeContenu(file.getContentType())
                .typeDocument(parseEnum(TypeDocument.class, typeDoc, TypeDocument.AUTRE))
                .description(description)
                .build();

        return documentRepo.save(doc);
    }

    public List<DocumentPatient> getDocuments(Long patientId) {
        return documentRepo.findByDossierMedicalPatientId(patientId);
    }

    public Path getDocumentFile(Long documentId) {
        DocumentPatient doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document non trouvé: " + documentId));
        return Paths.get(doc.getCheminFichier());
    }

    public void deleteDocument(Long documentId) {
        DocumentPatient doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document non trouvé: " + documentId));
        try { Files.deleteIfExists(Paths.get(doc.getCheminFichier())); } catch (IOException e) {
            log.warn("[DOC] Échec suppression fichier: {}", e.getMessage());
        }
        documentRepo.delete(doc);
    }

    // ── Messagerie ─────────────────────────────────────────────────────────
    public MessagePatient sendMessage(Long patientId, String expediteur,
                                      String medecinId, String medecinNom, String contenu) {
        return messageRepo.save(MessagePatient.builder()
                .patientId(patientId)
                .expediteur(expediteur)
                .medecinId(medecinId)
                .medecinNom(medecinNom)
                .contenu(contenu)
                .build());
    }

    public List<MessagePatient> getMessages(Long patientId) {
        return messageRepo.findByPatientIdOrderByEnvoyeAtDesc(patientId);
    }

    public MessagePatient markAsRead(Long messageId) {
        MessagePatient msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message non trouvé: " + messageId));
        msg.setLu(true);
        return messageRepo.save(msg);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Fichier vide");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Fichier trop grand (max 5MB)");
        String ct = file.getContentType();
        Set<String> allowed = Set.of("application/pdf","image/jpeg","image/png","image/gif","image/bmp","image/webp");
        if (ct == null || !allowed.contains(ct))
            throw new IllegalArgumentException("Type de fichier non autorisé: " + ct);
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private Double dbl(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    private LocalDate parseDate(Map<String, Object> m, String key) {
        String s = str(m, key);
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.substring(0, 10)); } catch (Exception e) { return null; }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val) {
        if (val == null) return null;
        try { return Enum.valueOf(cls, val.toUpperCase()); } catch (Exception e) { return null; }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val, E defaultVal) {
        E r = parseEnum(cls, val);
        return r != null ? r : defaultVal;
    }
}
