package com.hospital.patient.messaging;

import com.hospital.patient.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPatientNotification(Long patientId, String subject,
                                            String message, Long appointmentId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("PATIENT_NOTIFICATION")
                .patientId(patientId)
                .subject(subject)
                .message(message)
                .appointmentId(appointmentId)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PATIENT_EXCHANGE,
                    RabbitMQConfig.ROUTING_PATIENT_NOTIFICATION,
                    event);
            log.info("[RabbitMQ] Published PATIENT_NOTIFICATION for patientId={}", patientId);
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish PATIENT_NOTIFICATION: {}", e.getMessage());
        }
    }

    public void publishMessageSent(Long recipientId, String senderName, String messagePreview) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MESSAGE_SENT");
        event.put("recipientId", recipientId);
        event.put("senderName", senderName);
        event.put("messagePreview", messagePreview != null && messagePreview.length() > 60
                ? messagePreview.substring(0, 60) + "…"
                : messagePreview);
        event.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PATIENT_EXCHANGE,
                    RabbitMQConfig.ROUTING_MESSAGE_SENT,
                    event);
            log.info("[RabbitMQ] Published MESSAGE_SENT → recipientId={}", recipientId);
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish MESSAGE_SENT: {}", e.getMessage());
        }
    }

    public void publishRebookRequest(Long patientId, Long originalAppointmentId,
                                      Long doctorId, String doctorName,
                                      String specialty, String notes) {
        RebookEvent event = RebookEvent.builder()
                .eventType("APPOINTMENT_REBOOK_REQUESTED")
                .patientId(patientId)
                .originalAppointmentId(originalAppointmentId)
                .doctorId(doctorId)
                .doctorName(doctorName)
                .specialty(specialty)
                .notes(notes)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PATIENT_EXCHANGE,
                    RabbitMQConfig.ROUTING_APPOINTMENT_REBOOK,
                    event);
            log.info("[RabbitMQ] Published APPOINTMENT_REBOOK_REQUESTED patientId={} originalId={}",
                    patientId, originalAppointmentId);
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish APPOINTMENT_REBOOK_REQUESTED: {}", e.getMessage());
        }
    }
}
