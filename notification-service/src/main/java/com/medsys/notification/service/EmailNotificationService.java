package com.medsys.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendAppointmentConfirmation(String to, String patientNom, String medecinNom,
                                            String dateHeure, Long appointmentId) {
        send(to,
             "MedSys — Confirmation de votre rendez-vous",
             String.format("""
                Bonjour %s,

                Votre rendez-vous a été confirmé :
                - Médecin : %s
                - Date et heure : %s
                - Référence : RDV-%d

                Pour annuler ou modifier, connectez-vous sur : %s

                L'équipe MedSys
                """, patientNom, medecinNom, dateHeure, appointmentId, frontendUrl));
    }

    @Async
    public void sendAppointmentCancellation(String to, String patientNom, String dateHeure) {
        send(to,
             "MedSys — Annulation de rendez-vous",
             String.format("""
                Bonjour %s,

                Votre rendez-vous prévu le %s a été annulé.

                Vous pouvez reprogrammer sur : %s

                L'équipe MedSys
                """, patientNom, dateHeure, frontendUrl));
    }

    @Async
    public void sendAppointmentReminder(String to, String patientNom, String medecinNom, String dateHeure) {
        send(to,
             "MedSys — Rappel de rendez-vous demain",
             String.format("""
                Bonjour %s,

                Rappel : vous avez un rendez-vous demain.
                - Médecin : %s
                - Date et heure : %s

                En cas d'empêchement, veuillez annuler sur : %s

                L'équipe MedSys
                """, patientNom, medecinNom, dateHeure, frontendUrl));
    }

    @Async
    public void sendCustomEmail(String to, String subject, String body) {
        send(to, subject, body);
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("[EMAIL] Envoyé '{}' à {}", subject, to);
        } catch (Exception e) {
            log.warn("[EMAIL] Échec envoi à {}: {}", to, e.getMessage());
        }
    }
}
