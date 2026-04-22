package com.medsys.notification.messaging;

import com.medsys.notification.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final EmailNotificationService emailService;

    @RabbitListener(queues = "appointment.confirmed")
    public void onAppointmentConfirmed(Map<String, Object> event) {
        try {
            log.info("[NOTIFY] appointment.confirmed: {}", event);
            String email = str(event, "patientEmail");
            if (email != null) {
                emailService.sendAppointmentConfirmation(
                        email,
                        str(event, "patientNom"),
                        str(event, "medecinNom"),
                        str(event, "dateHeure"),
                        longVal(event, "appointmentId")
                );
            }
        } catch (Exception e) {
            log.warn("[NOTIFY] Erreur appointment.confirmed: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "appointment.cancelled")
    public void onAppointmentCancelled(Map<String, Object> event) {
        try {
            log.info("[NOTIFY] appointment.cancelled: {}", event);
            String email = str(event, "patientEmail");
            if (email != null) {
                emailService.sendAppointmentCancellation(
                        email,
                        str(event, "patientNom"),
                        str(event, "dateHeure")
                );
            }
        } catch (Exception e) {
            log.warn("[NOTIFY] Erreur appointment.cancelled: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "user.created")
    public void onUserCreated(Map<String, Object> event) {
        log.info("[NOTIFY] user.created: userId={}, role={}", event.get("userId"), event.get("role"));
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : null;
    }

    private Long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
