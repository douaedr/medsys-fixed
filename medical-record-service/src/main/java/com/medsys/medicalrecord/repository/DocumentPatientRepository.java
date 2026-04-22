package com.medsys.medicalrecord.repository;

import com.medsys.medicalrecord.entity.DocumentPatient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentPatientRepository extends JpaRepository<DocumentPatient, Long> {
    List<DocumentPatient> findByDossierMedicalPatientId(Long patientId);
}
