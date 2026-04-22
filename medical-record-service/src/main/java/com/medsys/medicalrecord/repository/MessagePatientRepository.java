package com.medsys.medicalrecord.repository;

import com.medsys.medicalrecord.entity.MessagePatient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessagePatientRepository extends JpaRepository<MessagePatient, Long> {
    List<MessagePatient> findByPatientIdOrderByEnvoyeAtDesc(Long patientId);
    long countByPatientIdAndLuFalse(Long patientId);
}
