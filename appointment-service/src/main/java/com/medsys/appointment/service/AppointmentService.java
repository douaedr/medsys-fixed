package com.medsys.appointment.service;

import com.medsys.appointment.config.RabbitMQConfig;
import com.medsys.appointment.entity.Appointment;
import com.medsys.appointment.entity.TimeSlot;
import com.medsys.appointment.enums.AppointmentPriority;
import com.medsys.appointment.enums.AppointmentStatus;
import com.medsys.appointment.repository.AppointmentRepository;
import com.medsys.appointment.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final TimeSlotRepository slotRepo;
    private final RabbitTemplate rabbitTemplate;

    public Appointment createAppointment(Map<String, Object> req) {
        Long creneauId = longVal(req, "creneauId");
        if (creneauId == null) throw new IllegalArgumentException("creneauId requis");

        TimeSlot slot = slotRepo.findById(creneauId)
                .orElseThrow(() -> new NoSuchElementException("Créneau introuvable: " + creneauId));
        if (!slot.isDisponible())
            throw new IllegalStateException("Ce créneau n'est plus disponible.");

        slot.setDisponible(false);
        slotRepo.save(slot);

        Appointment appointment = Appointment.builder()
                .patientId(longVal(req, "patientId"))
                .medecinId(longVal(req, "medecinId"))
                .creneauId(creneauId)
                .status(AppointmentStatus.PENDING)
                .priority(parseEnum(AppointmentPriority.class, str(req, "priority"), AppointmentPriority.NORMAL))
                .dateHeure(slot.getDebut())
                .motif(str(req, "motif"))
                .patientNom(str(req, "patientNom"))
                .patientPrenom(str(req, "patientPrenom"))
                .medecinNom(str(req, "medecinNom"))
                .medecinPrenom(str(req, "medecinPrenom"))
                .specialiteId(longVal(req, "specialiteId"))
                .build();

        appointment = appointmentRepo.save(appointment);
        log.info("[RDV] Créé: id={}, patient={}, medecin={}", appointment.getId(),
                appointment.getPatientId(), appointment.getMedecinId());

        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_CREATED, "appointment.created", appointment);
        return appointment;
    }

    public Appointment getById(Long id) {
        return appointmentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rendez-vous non trouvé: " + id));
    }

    public List<Appointment> getByPatientId(Long patientId) {
        return appointmentRepo.findByPatientId(patientId);
    }

    public List<Appointment> getByMedecinId(Long medecinId) {
        return appointmentRepo.findByMedecinId(medecinId);
    }

    public Appointment confirm(Long id) {
        Appointment appointment = getById(id);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment = appointmentRepo.save(appointment);
        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_CONFIRMED, "appointment.confirmed", appointment);
        log.info("[RDV] Confirmé: id={}", id);
        return appointment;
    }

    public Appointment cancel(Long id, String reason) {
        Appointment appointment = getById(id);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) appointment.setNotes(reason);
        appointment = appointmentRepo.save(appointment);

        if (appointment.getCreneauId() != null) {
            slotRepo.findById(appointment.getCreneauId()).ifPresent(slot -> {
                slot.setDisponible(true);
                slotRepo.save(slot);
                log.info("[RDV] Créneau libéré: slotId={}", slot.getId());
            });
        }

        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_CANCELLED, "appointment.cancelled", appointment);
        log.info("[RDV] Annulé: id={}", id);
        return appointment;
    }

    public Appointment complete(Long id) {
        Appointment appointment = getById(id);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepo.save(appointment);
    }

    public Appointment markNoShow(Long id) {
        Appointment appointment = getById(id);
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointment.setNoShowCount(appointment.getNoShowCount() + 1);
        appointment = appointmentRepo.save(appointment);
        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_NOSHOW, "appointment.noshow", appointment);
        return appointment;
    }

    public Map<String, Object> getStats() {
        long total = appointmentRepo.count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("pending", appointmentRepo.countByStatus(AppointmentStatus.PENDING));
        stats.put("confirmed", appointmentRepo.countByStatus(AppointmentStatus.CONFIRMED));
        stats.put("completed", appointmentRepo.countByStatus(AppointmentStatus.COMPLETED));
        stats.put("cancelled", appointmentRepo.countByStatus(AppointmentStatus.CANCELLED));
        stats.put("noShow", appointmentRepo.countByStatus(AppointmentStatus.NO_SHOW));
        stats.put("nouveauxCeMois", appointmentRepo.countByCreatedAtAfter(
                LocalDateTime.now().withDayOfMonth(1).withHour(0)));
        return stats;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void publishEvent(String queue, String routingKey, Appointment appointment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("appointmentId", appointment.getId());
            event.put("patientId", appointment.getPatientId());
            event.put("medecinId", appointment.getMedecinId());
            event.put("status", appointment.getStatus().name());
            event.put("dateHeure", appointment.getDateHeure().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
            log.info("[RABBITMQ] Published {} for appointmentId={}", routingKey, appointment.getId());
        } catch (Exception e) {
            log.warn("[RABBITMQ] Échec publication {}: {}", routingKey, e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : null;
    }

    private Long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val, E defaultVal) {
        if (val == null) return defaultVal;
        try { return Enum.valueOf(cls, val.toUpperCase()); } catch (Exception e) { return defaultVal; }
    }
}
