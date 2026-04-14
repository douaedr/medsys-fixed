package com.hospital.patient.messaging;

import com.hospital.patient.config.RabbitMQConfig;
import com.hospital.patient.dto.PatientRequestDTO;
import com.hospital.patient.repository.PatientRepository;
import com.hospital.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements publiés par ms-auth sur auth.exchange.
 *
 * <p>Quand un patient s'inscrit via ms-auth, ms-auth publie un
 * événement USER_CREATED. Ce listener crée automatiquement
 * le profil patient correspondant dans ms-patient-personnel.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {

    private final PatientService patientService;
    private final PatientRepository patientRepository;

    @RabbitListener(queues = RabbitMQConfig.PATIENT_AUTH_QUEUE)
    public void handleAuthEvent(AuthEventMessage event) {
        if (event == null || event.getEventType() == null) {
            log.warn("[RabbitMQ-Auth] Événement null ou invalide reçu");
            return;
        }

        log.info("[RabbitMQ-Auth] Événement reçu: type={} userId={} role={}",
                event.getEventType(), event.getUserId(), event.getRole());

        switch (event.getEventType()) {
            case "USER_CREATED" -> handleUserCreated(event);
            default -> log.debug("[RabbitMQ-Auth] Événement ignoré: {}", event.getEventType());
        }
    }

    private void handleUserCreated(AuthEventMessage event) {
        // Uniquement les patients ont un profil dans ce microservice
        if (!"PATIENT".equalsIgnoreCase(event.getRole())) {
            log.debug("[RabbitMQ-Auth] Rôle non-patient ignoré: {}", event.getRole());
            return;
        }

        // Vérifier si le patient existe déjà (éviter les doublons en cas de re-delivery)
        if (event.getEmail() != null && patientRepository.existsByEmail(event.getEmail())) {
            log.warn("[RabbitMQ-Auth] Patient avec email {} existe déjà, ignoré", event.getEmail());
            return;
        }

        try {
            PatientRequestDTO dto = PatientRequestDTO.builder()
                    .nom(event.getNom() != null ? event.getNom() : "")
                    .prenom(event.getPrenom() != null ? event.getPrenom() : "")
                    // Le CIN n'est pas disponible dans l'event auth, on utilise l'userId comme fallback
                    .cin("AUTH-" + event.getUserId())
                    .email(event.getEmail())
                    .build();

            patientService.createPatient(dto);

            log.info("[RabbitMQ-Auth] Profil patient créé pour userId={} email={}",
                    event.getUserId(), event.getEmail());

        } catch (Exception e) {
            log.error("[RabbitMQ-Auth] Erreur lors de la création du profil patient pour userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
