package com.medsys.auth.service;

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
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String to, String nom, String token) {
        String link = frontendUrl + "/verify?token=" + token;
        send(to, "MedSys — Vérification de votre compte",
            "Bonjour " + nom + ",\n\n"
            + "Cliquez sur le lien ci-dessous pour vérifier votre adresse email :\n"
            + link + "\n\n"
            + "Ce lien expire dans 24 heures.\n\n"
            + "L'équipe MedSys");
    }

    @Async
    public void sendPasswordResetEmail(String to, String nom, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(to, "MedSys — Réinitialisation de mot de passe",
            "Bonjour " + nom + ",\n\n"
            + "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe :\n"
            + link + "\n\n"
            + "Ce lien expire dans 1 heure.\n\n"
            + "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n"
            + "L'équipe MedSys");
    }

    @Async
    public void sendAccountCreatedEmail(String to, String nom, String temporaryPassword) {
        send(to, "MedSys — Votre compte a été créé",
            "Bonjour " + nom + ",\n\n"
            + "Votre compte MedSys a été créé par l'administrateur.\n\n"
            + "Email : " + to + "\n"
            + "Mot de passe temporaire : " + temporaryPassword + "\n\n"
            + "Connectez-vous sur : " + frontendUrl + "\n"
            + "Pensez à changer votre mot de passe après la première connexion.\n\n"
            + "L'équipe MedSys");
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("[EMAIL] Sent '{}' to {}", subject, to);
        } catch (Exception e) {
            log.warn("[EMAIL] Failed to send to {}: {}", to, e.getMessage());
        }
    }
}
