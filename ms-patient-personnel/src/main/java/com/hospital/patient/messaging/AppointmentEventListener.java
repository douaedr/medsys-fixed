package com.hospital.patient.messaging;

import com.hospital.patient.config.RabbitMQConfig;
import com.hospital.patient.entity.AppointmentRecord;
import com.hospital.patient.repository.AppointmentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private final AppointmentRecordRepository appointmentRepo;
    private final NotificationPublisher notificationPublisher;

    @RabbitListener(queues = RabbitMQConfig.PATIENT_QUEUE)
    public void handleAppointmentEvent(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("eventType")) {
            log.warn("[RabbitMQ] Received null or invalid appointment event payload");
            return;
        }

        String eventType    = getString(payload, "eventType");
        Long appointmentId  = getLong(payload, "appointmentId");
        Long patientId      = getLong(payload, "patientId");
        Long doctorId       = getLong(payload, "doctorId");
        String doctorName   = getString(payload, "doctorName");
        String specialty    = getString(payload, "specialty");
        String dateStr      = getString(payload, "appointmentDate");
        String notes        = getString(payload, "notes");

        LocalDateTime appointmentDate = null;
        if (dateStr != null) {
            try { appointmentDate = LocalDateTime.parse(dateStr); } catch (Exception ignored) {}
        }

        log.info("[RabbitMQ] Received event: type={} appointmentId={} patientId={}",
                eventType, appointmentId, patientId);

        switch (eventType != null ? eventType : "") {
            case "APPOINTMENT_CREATED"   -> handleCreated(appointmentId, patientId, doctorId,
                                                doctorName, specialty, appointmentDate, notes);
            case "APPOINTMENT_CANCELLED" -> handleCancelled(appointmentId, patientId);
            default -> log.warn("[RabbitMQ] Unknown appointment event type: {}", eventType);
        }
    }

    private void handleCreated(Long appointmentId, Long patientId, Long doctorId,
                                String doctorName, String specialty,
                                LocalDateTime appointmentDate, String notes) {
        AppointmentRecord record = appointmentRepo
                .findByExternalAppointmentId(appointmentId)
                .orElse(AppointmentRecord.builder()
                        .externalAppointmentId(appointmentId)
                        .build());

        record.setPatientId(patientId);
        record.setDoctorId(doctorId);
        record.setDoctorName(doctorName);
        record.setSpecialty(specialty);
        record.setAppointmentDate(appointmentDate);
        record.setStatus("SCHEDULED");
        record.setNotes(notes);
        record.setUpdatedAt(LocalDateTime.now());

        appointmentRepo.save(record);

        notificationPublisher.publishPatientNotification(
                patientId,
                "Rendez-vous confirme",
                "Votre rendez-vous avec " + doctorName + " est confirme pour le " + appointmentDate,
                appointmentId
        );
    }

    private void handleCancelled(Long appointmentId, Long patientId) {
        appointmentRepo.findByExternalAppointmentId(appointmentId)
                .ifPresent(record -> {
                    record.setStatus("CANCELLED");
                    record.setUpdatedAt(LocalDateTime.now());
                    appointmentRepo.save(record);

                    notificationPublisher.publishPatientNotification(
                            patientId,
                            "Rendez-vous annule",
                            "Votre rendez-vous avec " + record.getDoctorName()
                                    + " du " + record.getAppointmentDate() + " a ete annule.",
                            appointmentId
                    );
                });
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
