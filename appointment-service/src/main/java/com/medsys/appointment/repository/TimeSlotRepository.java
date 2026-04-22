package com.medsys.appointment.repository;

import com.medsys.appointment.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByMedecinIdAndDisponibleTrue(Long medecinId);
    List<TimeSlot> findByMedecinIdAndDebutAfterAndDisponibleTrue(Long medecinId, LocalDateTime after);
    List<TimeSlot> findByDisponibleTrueAndDebutAfter(LocalDateTime after);
}
