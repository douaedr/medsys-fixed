package com.hospital.patient.messaging;

import com.hospital.patient.config.RabbitMQConfig;
import com.hospital.patient.entity.AppointmentRecord;
import com.hospital.patient.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Consommateur RabbitMQ pour les événements de rendez-vous publiés par ms-rdv.
 *
 * <p>Écoute sur {@code patient.rdv.queue}, lié à {@code medsys.exchange} avec
 * les routing keys {@code appointment.created} et {@code appointment.cancelled}.
 * Cette queue est séparée des queues de ms-notify pour que les deux services
 * reçoivent chaque message indépendamment (pas de compétition).</p>
 *
 * <p>Responsabilité : maintenir une copie locale des rendez-vous pour les
 * requêtes de tableau de bord sans appels synchrones vers ms-rdv.
 * Les notifications WebSocket/email sont gérées directement par ms-notify.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private final AppointmentRecordRepository appointmentRepo;

    @RabbitListener(queues = RabbitMQConfig.PATIENT_RDV_QUEUE)
    public void handleAppointmentEvent(AppointmentEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("[RabbitMQ] Événement reçu null ou sans eventType — ignoré");
            return;
        }

        log.info("[RabbitMQ] Événement reçu : type={} appointmentId={} patientId={}",
                event.getEventType(), event.getAppointmentId(), event.getPatientId());

        switch (event.getEventType()) {
            case "APPOINTMENT_CREATED"   -> handleCreated(event);
            case "APPOINTMENT_CANCELLED" -> handleCancelled(event);
            case "APPOINTMENT_NOSHOW"    -> handleNoShow(event);
            default -> log.warn("[RabbitMQ] Type d'événement inconnu : {}", event.getEventType());
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleCreated(AppointmentEvent event) {
        AppointmentRecord record = appointmentRepo
                .findByExternalAppointmentId(event.getAppointmentId())
                .orElse(AppointmentRecord.builder()
                        .externalAppointmentId(event.getAppointmentId())
                        .build());

        record.setPatientId(event.getPatientId());
        record.setDoctorId(event.getMedecinId());                     // medecinId → doctorId
        record.setDoctorName(buildFullName(event.getMedecinNom(),     // medecinNom → doctorName
                                           event.getMedecinPrenom()));
        record.setAppointmentDate(parseDate(event.getDateHeure(),     // dateHeure (String) → LocalDateTime
                                            event.getAppointmentId()));
        record.setStatus("SCHEDULED");
        record.setNotes(event.getNotes());
        record.setUpdatedAt(LocalDateTime.now());

        appointmentRepo.save(record);
        log.info("[RabbitMQ] Rendez-vous {} enregistré localement pour patient {}",
                event.getAppointmentId(), event.getPatientId());
        // La notification WebSocket/email est gérée par ms-notify directement.
    }

    private void handleCancelled(AppointmentEvent event) {
        appointmentRepo.findByExternalAppointmentId(event.getAppointmentId())
                .ifPresentOrElse(record -> {
                    record.setStatus("CANCELLED");
                    record.setUpdatedAt(LocalDateTime.now());
                    appointmentRepo.save(record);
                    log.info("[RabbitMQ] Rendez-vous {} annulé localement", event.getAppointmentId());
                    // La notification WebSocket/email est gérée par ms-notify directement.
                }, () -> log.warn("[RabbitMQ] Rendez-vous {} introuvable pour annulation",
                        event.getAppointmentId()));
    }

    private void handleNoShow(AppointmentEvent event) {
        appointmentRepo.findByExternalAppointmentId(event.getAppointmentId())
                .ifPresent(record -> {
                    record.setStatus("NO_SHOW");
                    record.setUpdatedAt(LocalDateTime.now());
                    appointmentRepo.save(record);
                    log.info("[RabbitMQ] No-show enregistré pour rendez-vous {}", event.getAppointmentId());
                });
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String buildFullName(String nom, String prenom) {
        if (nom == null && prenom == null) return null;
        return (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
    }

    private LocalDateTime parseDate(String dateHeure, Long appointmentId) {
        if (dateHeure == null || dateHeure.isBlank()) {
            log.warn("[RabbitMQ] dateHeure manquant pour rendez-vous {}", appointmentId);
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateHeure);
        } catch (DateTimeParseException e) {
            log.warn("[RabbitMQ] Format dateHeure invalide '{}' pour rendez-vous {} : {}",
                    dateHeure, appointmentId, e.getMessage());
            return LocalDateTime.now();
        }
    }
}
