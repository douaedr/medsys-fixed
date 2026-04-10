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

import java.time.LocalDateTime;
import java.util.HashMap;
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

        publishEvent("APPOINTMENT_CREATED", RabbitMQConfig.QUEUE_APPOINTMENT_CREATED, "appointment.created", appointment);

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
        publishEvent("APPOINTMENT_CONFIRMED", RabbitMQConfig.QUEUE_APPOINTMENT_CONFIRMED, "appointment.confirmed", appointment);
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
        publishEvent("APPOINTMENT_CANCELLED", RabbitMQConfig.QUEUE_APPOINTMENT_CANCELLED, "appointment.cancelled", appointment);
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

        publishEvent("APPOINTMENT_NOSHOW", RabbitMQConfig.QUEUE_APPOINTMENT_NOSHOW, "appointment.noshow", appointment);
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

    /**
     * Publie un événement enrichi sur medsys.exchange.
     *
     * @param eventType  Type d'événement (ex: "APPOINTMENT_CREATED")
     * @param queue      Nom de la queue (non utilisé directement, pour log)
     * @param routingKey Routing key RabbitMQ
     * @param apt        Entité Appointment source
     */
    private void publishEvent(String eventType, String queue, String routingKey, Appointment apt) {
        try {
            Map<String, Object> payload = new HashMap<>();
            // Champs obligatoires
            payload.put("eventType",     eventType);
            payload.put("appointmentId", apt.getId());
            payload.put("patientId",     apt.getPatientId());
            payload.put("medecinId",     apt.getMedecinId());
            payload.put("dateHeure",     apt.getDateHeure().toString());
            payload.put("status",        apt.getStatus().name());
            payload.put("timestamp",     LocalDateTime.now().toString());
            // Noms en clair — disponibles car stockés à la création du RDV
            payload.put("patientNom",    apt.getPatientNom());
            payload.put("patientPrenom", apt.getPatientPrenom());
            payload.put("medecinNom",    apt.getMedecinNom());
            payload.put("medecinPrenom", apt.getMedecinPrenom());
            // Informations complémentaires
            payload.put("motif",         apt.getMotif());
            payload.put("notes",         apt.getNotes());
            payload.put("noShowCount",   apt.getNoShowCount());

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload);
            log.info("[RabbitMQ] Événement '{}' publié pour rendez-vous id={}", eventType, apt.getId());
        } catch (Exception ex) {
            log.warn("[RabbitMQ] Échec publication '{}' pour rendez-vous id={} : {}",
                    routingKey, apt.getId(), ex.getMessage());
        }
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
