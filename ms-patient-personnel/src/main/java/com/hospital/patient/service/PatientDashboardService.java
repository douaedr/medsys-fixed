package com.hospital.patient.service;

import com.hospital.patient.dto.AppointmentRecordDTO;
import com.hospital.patient.dto.PatientDashboardDTO;
import com.hospital.patient.entity.AppointmentRecord;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.mapper.PatientMapper;
import com.hospital.patient.repository.AppointmentRecordRepository;
import com.hospital.patient.repository.FavoriteDoctorRepository;
import com.hospital.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientDashboardService {

    private final PatientRepository patientRepo;
    private final AppointmentRecordRepository appointmentRepo;
    private final FavoriteDoctorRepository favoriteRepo;
    private final MessageService messageService;
    private final PatientMapper patientMapper;

    public PatientDashboardDTO getDashboard(Long patientId) {
        var patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient introuvable: " + patientId));

        LocalDateTime now = LocalDateTime.now();

        List<AppointmentRecordDTO> upcoming = appointmentRepo
                .findByPatientIdAndAppointmentDateAfterOrderByAppointmentDateAsc(patientId, now)
                .stream().map(this::toDTO).collect(Collectors.toList());

        List<AppointmentRecordDTO> past = appointmentRepo
                .findByPatientIdAndAppointmentDateBeforeOrderByAppointmentDateDesc(patientId, now)
                .stream().map(this::toDTO).collect(Collectors.toList());

        long totalAppointments = appointmentRepo.findByPatientIdOrderByAppointmentDateDesc(patientId).size();
        long completed   = appointmentRepo.countByPatientIdAndStatus(patientId, "COMPLETED");
        long cancelled   = appointmentRepo.countByPatientIdAndStatus(patientId, "CANCELLED");
        long favorites   = favoriteRepo.findByPatientId(patientId).size();
        long unread      = messageService.countUnreadFromMedecin(patientId);

        return PatientDashboardDTO.builder()
                .profile(patientMapper.toResponseDTO(patient))
                .upcomingAppointments(upcoming)
                .pastAppointments(past)
                .statistics(PatientDashboardDTO.DashboardStatsDTO.builder()
                        .totalAppointments(totalAppointments)
                        .upcomingCount(upcoming.size())
                        .completedCount(completed)
                        .cancelledCount(cancelled)
                        .favoriteDoctorsCount(favorites)
                        .unreadMessages(unread)
                        .build())
                .build();
    }

    public List<AppointmentRecordDTO> getAppointmentHistory(Long patientId) {
        if (!patientRepo.existsById(patientId)) {
            throw new PatientNotFoundException("Patient introuvable: " + patientId);
        }
        return appointmentRepo.findByPatientIdOrderByAppointmentDateDesc(patientId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AppointmentRecordDTO toDTO(AppointmentRecord r) {
        return AppointmentRecordDTO.builder()
                .id(r.getId())
                .externalAppointmentId(r.getExternalAppointmentId())
                .doctorId(r.getDoctorId())
                .doctorName(r.getDoctorName())
                .specialty(r.getSpecialty())
                .appointmentDate(r.getAppointmentDate())
                .status(r.getStatus())
                .notes(r.getNotes())
                .recordedAt(r.getRecordedAt())
                .build();
    }
}
