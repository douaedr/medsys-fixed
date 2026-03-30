package com.hospital.patient.service;

import com.hospital.patient.entity.RendezVous;
import com.hospital.patient.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service de rappels automatiques : envoie un email 24h avant chaque RDV.
 * Exécuté toutes les heures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RappelRdvService {

    private final RendezVousRepository rdvRepository;
    private final JavaMailSender mailSender;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    @Scheduled(cron = "0 0 * * * *") // toutes les heures
    @Transactional
    public void envoyerRappels() {
        LocalDateTime debut = LocalDateTime.now().plusHours(23);
        LocalDateTime fin   = LocalDateTime.now().plusHours(25);

        List<RendezVous> rdvAVenir = rdvRepository.findRdvForRappel(debut, fin);
        log.info("Rappels RDV : {} rendez-vous à rappeler", rdvAVenir.size());

        for (RendezVous rdv : rdvAVenir) {
            try {
                envoyerEmailRappel(rdv);
                rdv.setRappelEnvoye(true);
                rdvRepository.save(rdv);
            } catch (Exception e) {
                log.warn("Impossible d'envoyer le rappel pour RDV {} : {}", rdv.getId(), e.getMessage());
            }
        }
    }

    private void envoyerEmailRappel(RendezVous rdv) {
        String emailPatient = rdv.getPatient().getEmail();
        if (emailPatient == null || emailPatient.isBlank()) {
            log.debug("Pas d'email pour le patient {}, rappel ignoré", rdv.getPatient().getId());
            return;
        }

        String nomPatient = rdv.getPatient().getPrenom() + " " + rdv.getPatient().getNom();
        String dateFormatee = rdv.getDateHeure().format(FMT);
        String medecin = rdv.getMedecin() != null ? rdv.getMedecin().getNomComplet() : "votre médecin";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailPatient);
        message.setSubject("Rappel : Rendez-vous médical demain - MedSys");
        message.setText(
            "Bonjour " + nomPatient + ",\n\n" +
            "Nous vous rappelons votre rendez-vous médical :\n" +
            "  Date et heure : " + dateFormatee + "\n" +
            "  Médecin       : " + medecin + "\n" +
            (rdv.getService() != null ? "  Service       : " + rdv.getService() + "\n" : "") +
            (rdv.getLieu() != null    ? "  Lieu          : " + rdv.getLieu() + "\n" : "") +
            "  Motif         : " + rdv.getMotif() + "\n\n" +
            "Merci de vous présenter 10 minutes avant votre rendez-vous.\n" +
            "Pour annuler ou modifier votre rendez-vous, connectez-vous sur votre espace patient MedSys.\n\n" +
            "Cordialement,\nL'équipe MedSys"
        );
        mailSender.send(message);
        log.info("Rappel RDV envoyé à {} pour le {}", emailPatient, dateFormatee);
    }
}
