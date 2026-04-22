package com.medsys.appointment.repository;

import com.medsys.appointment.entity.Appointment;
import com.medsys.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByMedecinId(Long medecinId);
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByDateHeureBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(AppointmentStatus status);
    long countByCreatedAtAfter(LocalDateTime date);
}
