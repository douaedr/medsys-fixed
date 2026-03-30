package com.hospital.patient.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service SSE (Server-Sent Events) pour les notifications temps réel.
 * Chaque utilisateur connecté reçoit les événements qui le concernent.
 */
@Slf4j
@Service
public class NotificationService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long patientId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        emitter.onCompletion(() -> {
            emitters.remove(patientId);
            log.debug("SSE emitter fermé pour patient {}", patientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(patientId);
            log.debug("SSE emitter timeout pour patient {}", patientId);
        });
        emitter.onError(e -> {
            emitters.remove(patientId);
            log.debug("SSE emitter erreur pour patient {}: {}", patientId, e.getMessage());
        });

        emitters.put(patientId, emitter);
        log.debug("Patient {} abonné aux notifications SSE", patientId);

        // Envoyer un événement de connexion
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "Connecté aux notifications", "patientId", patientId)));
        } catch (IOException e) {
            emitters.remove(patientId);
        }

        return emitter;
    }

    public void notifierPatient(Long patientId, String type, Object data) {
        SseEmitter emitter = emitters.get(patientId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(data));
            log.debug("Notification '{}' envoyée au patient {}", type, patientId);
        } catch (IOException e) {
            emitters.remove(patientId);
            log.debug("Impossible d'envoyer la notification au patient {}: {}", patientId, e.getMessage());
        }
    }

    public void notifierNouveauMessage(Long patientId, String expediteur, String extrait) {
        notifierPatient(patientId, "nouveau_message", Map.of(
                "type", "nouveau_message",
                "expediteur", expediteur,
                "extrait", extrait.length() > 100 ? extrait.substring(0, 100) + "..." : extrait
        ));
    }

    public void notifierResultatAnalyse(Long patientId, String typeAnalyse) {
        notifierPatient(patientId, "resultat_analyse", Map.of(
                "type", "resultat_analyse",
                "typeAnalyse", typeAnalyse,
                "message", "Votre résultat d'analyse " + typeAnalyse + " est disponible"
        ));
    }

    public void notifierRdvConfirme(Long patientId, String dateHeure, String medecin) {
        notifierPatient(patientId, "rdv_confirme", Map.of(
                "type", "rdv_confirme",
                "dateHeure", dateHeure,
                "medecin", medecin != null ? medecin : "à définir",
                "message", "Votre rendez-vous du " + dateHeure + " est confirmé"
        ));
    }

    public void notifierRdvAnnule(Long patientId, String dateHeure) {
        notifierPatient(patientId, "rdv_annule", Map.of(
                "type", "rdv_annule",
                "dateHeure", dateHeure,
                "message", "Votre rendez-vous du " + dateHeure + " a été annulé"
        ));
    }

    public int countConnectedPatients() {
        return emitters.size();
    }
}
