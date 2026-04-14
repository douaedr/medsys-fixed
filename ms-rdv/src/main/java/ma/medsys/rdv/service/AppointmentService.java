package ma.medsys.rdv.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.medsys.rdv.config.RabbitMQConfig;
import ma.medsys.rdv.dto.AppointmentRequest;
import ma.medsys.rdv.dto.AppointmentResponse;
import ma.medsys.rdv.entity.Appointment;
import ma.medsys.rdv.entity.TimeSlot;
import ma.medsys.rdv.enums.AppointmentPriority;
import ma.medsys.rdv.enums.AppointmentStatus;
import ma.medsys.rdv.repository.AppointmentRepository;
import ma.medsys.rdv.repository.TimeSlotRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final TimeSlotRepository slotRepo;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest req) {
        TimeSlot slot = slotRepo.findById(req.getCreneauId())
                .orElseThrow(() -> new NoSuchElementException("Créneau introuvable: " + req.getCreneauId()));

        if (!slot.isDisponible()) {
            throw new IllegalStateException("Ce créneau n'est plus disponible.");
        }

        // Mark slot as taken
        slot.setDisponible(false);
        slotRepo.save(slot);

        AppointmentPriority priority = req.getPriority() != null ? req.getPriority() : AppointmentPriority.NORMAL;

        Appointment appointment = Appointment.builder()
                .patientId(req.getPatientId())
                .medecinId(req.getMedecinId())
                .creneauId(req.getCreneauId())
                .status(AppointmentStatus.PENDING)
                .priority(priority)
                .dateHeure(slot.getDebut())
                .motif(req.getMotif())
                .patientNom(req.getPatientNom())
                .patientPrenom(req.getPatientPrenom())
                .medecinNom(req.getMedecinNom())
                .medecinPrenom(req.getMedecinPrenom())
                .specialiteId(req.getSpecialiteId())
                .rappelEnvoye(false)
                .noShowCount(0)
                .build();

        appointment = appointmentRepo.save(appointment);
        log.info("Appointment created: id={}, patient={}, medecin={}", appointment.getId(),
                appointment.getPatientId(), appointment.getMedecinId());

        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_CREATED, "appointment.created", appointment);

        return toResponse(appointment);
    }

    public AppointmentResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<AppointmentResponse> getByPatientId(Long patientId) {
        return appointmentRepo.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getByMedecinId(Long medecinId) {
        return appointmentRepo.findByMedecinId(medecinId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse confirm(Long id) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment = appointmentRepo.save(appointment);
        log.info("Appointment confirmed: id={}", id);
        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_CONFIRMED, "appointment.confirmed", appointment);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, String reason) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            appointment.setNotes(reason);
        }
        appointment = appointmentRepo.save(appointment);

        // Free the time slot again
        if (appointment.getCreneauId() != null) {
            slotRepo.findById(appointment.getCreneauId()).ifPresent(slot -> {
                slot.setDisponible(true);
                slotRepo.save(slot);
                log.info("Slot freed after cancellation: slotId={}", slot.getId());
            });
        }

        log.info("Appointment cancelled: id={}", id);
        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_CANCELLED, "appointment.cancelled", appointment);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse markNoShow(Long id) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(AppointmentStatus.NO_SHOW);

        // Increment noShowCount for all appointments of this patient (denormalized counter)
        List<Appointment> patientAppointments = appointmentRepo.findByPatientId(appointment.getPatientId());
        patientAppointments.forEach(a -> a.setNoShowCount(a.getNoShowCount() + 1));
        appointmentRepo.saveAll(patientAppointments);

        log.info("No-show recorded for patient={}, appointment={}", appointment.getPatientId(), id);

        publishEvent(RabbitMQConfig.QUEUE_APPOINTMENT_NOSHOW, "appointment.noshow", appointment);
        // Return the updated appointment from the batch save
        return toResponse(patientAppointments.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(appointment));
    }

    @Transactional
    public AppointmentResponse complete(Long id) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment = appointmentRepo.save(appointment);
        log.info("Appointment completed: id={}", id);
        return toResponse(appointment);
    }

    // ---- helpers ----

    private Appointment findOrThrow(Long id) {
        return appointmentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rendez-vous introuvable: " + id));
    }

    private void publishEvent(String queue, String routingKey, Appointment apt) {
        try {
            String medecinNomComplet  = buildFullName(apt.getMedecinNom(), apt.getMedecinPrenom());
            String patientNomComplet  = buildFullName(apt.getPatientNom(), apt.getPatientPrenom());
            String eventType = routingKey.toUpperCase().replace(".", "_"); // appointment.created → APPOINTMENT_CREATED

            // Payload unifié : contient les champs attendus par ms-notify ET ms-patient-personnel
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("eventType",       eventType);
            payload.put("appointmentId",   apt.getId());
            // Champs pour ms-notify
            payload.put("patientId",       apt.getPatientId());
            payload.put("medecinId",       apt.getMedecinId());
            payload.put("medecinNom",      medecinNomComplet);
            payload.put("patientNom",      patientNomComplet);
            payload.put("dateHeure",       apt.getDateHeure().toString());
            payload.put("patientEmail",    ""); // enrichi si disponible
            payload.put("noShowCount",     apt.getNoShowCount());
            // Champs pour ms-patient-personnel (AppointmentEvent)
            payload.put("doctorId",        apt.getMedecinId());
            payload.put("doctorName",      medecinNomComplet);
            payload.put("appointmentDate", apt.getDateHeure().toString());
            payload.put("specialty",       apt.getSpecialiteId() != null ? apt.getSpecialiteId().toString() : "");
            payload.put("notes",           apt.getMotif() != null ? apt.getMotif() : "");
            payload.put("timestamp",       java.time.LocalDateTime.now().toString());
            payload.put("status",          apt.getStatus().name());

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload);
            log.info("[RabbitMQ] Event publié: routingKey={} appointmentId={}", routingKey, apt.getId());
        } catch (Exception ex) {
            log.warn("[RabbitMQ] Échec publication event '{}': {}", routingKey, ex.getMessage());
        }
    }

    private String buildFullName(String nom, String prenom) {
        if (nom == null && prenom == null) return "";
        if (nom == null) return prenom;
        if (prenom == null) return nom;
        return nom + " " + prenom;
    }

    public AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .medecinId(a.getMedecinId())
                .creneauId(a.getCreneauId())
                .status(a.getStatus())
                .priority(a.getPriority())
                .dateHeure(a.getDateHeure())
                .motif(a.getMotif())
                .notes(a.getNotes())
                .rappelEnvoye(a.isRappelEnvoye())
                .noShowCount(a.getNoShowCount())
                .patientNom(a.getPatientNom())
                .patientPrenom(a.getPatientPrenom())
                .medecinNom(a.getMedecinNom())
                .medecinPrenom(a.getMedecinPrenom())
                .specialiteId(a.getSpecialiteId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .statusLabel(AppointmentResponse.computeStatusLabel(a.getStatus()))
                .build();
    }
}
