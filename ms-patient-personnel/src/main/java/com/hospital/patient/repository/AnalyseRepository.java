package com.hospital.patient.repository;

import com.hospital.patient.entity.AnalyseLaboratoire;
import com.hospital.patient.enums.StatutAnalyse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyseRepository extends JpaRepository<AnalyseLaboratoire, Long> {

    long countByStatut(StatutAnalyse statut);
}
