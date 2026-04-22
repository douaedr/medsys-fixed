package com.medsys.appointment.repository;

import com.medsys.appointment.entity.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitingListRepository extends JpaRepository<WaitingListEntry, Long> {
    List<WaitingListEntry> findByProcessedFalseOrderByCreatedAtAsc();
    List<WaitingListEntry> findByMedecinIdAndProcessedFalse(Long medecinId);
    List<WaitingListEntry> findByPatientId(Long patientId);
}
