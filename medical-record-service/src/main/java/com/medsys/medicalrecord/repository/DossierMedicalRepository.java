package com.medsys.medicalrecord.repository;

import com.medsys.medicalrecord.entity.DossierMedical;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DossierMedicalRepository extends JpaRepository<DossierMedical, Long> {
    Optional<DossierMedical> findByPatientId(Long patientId);
    boolean existsByPatientId(Long patientId);
}
