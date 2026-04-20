package ma.medsys.rdv.service;

import ma.medsys.rdv.dto.AppointmentRequest;
import ma.medsys.rdv.dto.AppointmentResponse;
import ma.medsys.rdv.entity.Appointment;
import ma.medsys.rdv.entity.TimeSlot;
import ma.medsys.rdv.enums.AppointmentPriority;
import ma.medsys.rdv.enums.AppointmentStatus;
import ma.medsys.rdv.repository.AppointmentRepository;
import ma.medsys.rdv.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private TimeSlotRepository slotRepo;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AppointmentService appointmentService;

    private TimeSlot availableSlot;
    private Appointment savedAppointment;

    @BeforeEach
    void setup() {
        availableSlot = TimeSlot.builder()
                .id(1L)
                .medecinId(10L)
                .debut(LocalDateTime.of(2026, 5, 15, 9, 0))
                .fin(LocalDateTime.of(2026, 5, 15, 9, 30))
                .disponible(true)
                .build();

        savedAppointment = Appointment.builder()
                .id(100L)
                .patientId(5L)
                .medecinId(10L)
                .creneauId(1L)
                .status(AppointmentStatus.PENDING)
                .priority(AppointmentPriority.NORMAL)
                .dateHeure(LocalDateTime.of(2026, 5, 15, 9, 0))
                .motif("Consultation générale")
                .patientNom("Alami")
                .patientPrenom("Fatima")
                .medecinNom("Hassan")
                .medecinPrenom("Dr")
                .rappelEnvoye(false)
                .noShowCount(0)
                .build();
    }

    @Test
    @DisplayName("createAppointment() - créneau disponible → rendez-vous créé + événement publié")
    void createAppointment_success() {
        AppointmentRequest req = AppointmentRequest.builder()
                .patientId(5L)
                .medecinId(10L)
                .creneauId(1L)
                .motif("Consultation générale")
                .patientNom("Alami")
                .patientPrenom("Fatima")
                .medecinNom("Hassan")
                .medecinPrenom("Dr")
                .priority(AppointmentPriority.NORMAL)
                .build();

        when(slotRepo.findById(1L)).thenReturn(Optional.of(availableSlot));
        when(appointmentRepo.save(any())).thenReturn(savedAppointment);
        when(slotRepo.save(any())).thenReturn(availableSlot);

        AppointmentResponse response = appointmentService.createAppointment(req);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(availableSlot.isDisponible()).isFalse();
        verify(rabbitTemplate).convertAndSend(anyString(), eq("appointment.created"), any(Object.class));
    }

    @Test
    @DisplayName("createAppointment() - créneau indisponible → IllegalStateException")
    void createAppointment_slotUnavailable() {
        availableSlot.setDisponible(false);
        when(slotRepo.findById(1L)).thenReturn(Optional.of(availableSlot));

        AppointmentRequest req = AppointmentRequest.builder()
                .creneauId(1L).patientId(5L).medecinId(10L).build();

        assertThatThrownBy(() -> appointmentService.createAppointment(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    @DisplayName("createAppointment() - créneau inexistant → NoSuchElementException")
    void createAppointment_slotNotFound() {
        when(slotRepo.findById(99L)).thenReturn(Optional.empty());

        AppointmentRequest req = AppointmentRequest.builder()
                .creneauId(99L).patientId(5L).medecinId(10L).build();

        assertThatThrownBy(() -> appointmentService.createAppointment(req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("confirm() - rendez-vous existant → statut CONFIRMED + événement publié")
    void confirm_success() {
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(savedAppointment));
        when(appointmentRepo.save(any())).thenReturn(savedAppointment);

        AppointmentResponse response = appointmentService.confirm(100L);

        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(rabbitTemplate).convertAndSend(anyString(), eq("appointment.confirmed"), any(Object.class));
    }

    @Test
    @DisplayName("cancel() - rendez-vous confirmé → statut CANCELLED + slot libéré")
    void cancel_success() {
        savedAppointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(savedAppointment));
        when(appointmentRepo.save(any())).thenReturn(savedAppointment);
        when(slotRepo.findById(1L)).thenReturn(Optional.of(availableSlot));
        when(slotRepo.save(any())).thenReturn(availableSlot);

        AppointmentResponse response = appointmentService.cancel(100L, "Empêchement");

        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(availableSlot.isDisponible()).isTrue();
        verify(rabbitTemplate).convertAndSend(anyString(), eq("appointment.cancelled"), any(Object.class));
    }

    @Test
    @DisplayName("getByPatientId() - liste des RDV du patient")
    void getByPatientId() {
        when(appointmentRepo.findByPatientId(5L)).thenReturn(List.of(savedAppointment));

        List<AppointmentResponse> result = appointmentService.getByPatientId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getAll() - liste complète pour le directeur")
    void getAll() {
        when(appointmentRepo.findAll()).thenReturn(List.of(savedAppointment));

        List<AppointmentResponse> result = appointmentService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("markNoShow() - RDV existant → statut NO_SHOW + compteur incrémenté")
    void markNoShow_success() {
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(savedAppointment));
        when(appointmentRepo.findByPatientId(5L)).thenReturn(List.of(savedAppointment));
        when(appointmentRepo.saveAll(anyList())).thenReturn(List.of(savedAppointment));

        AppointmentResponse response = appointmentService.markNoShow(100L);

        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(savedAppointment.getNoShowCount()).isEqualTo(1);
    }
}
