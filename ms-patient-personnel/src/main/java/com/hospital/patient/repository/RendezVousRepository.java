package com.hospital.patient.repository;

import com.hospital.patient.entity.RendezVous;
import com.hospital.patient.enums.StatutRdv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    List<RendezVous> findByPatientIdOrderByDateHeureDesc(Long patientId);

    List<RendezVous> findByMedecinIdOrderByDateHeureDesc(Long medecinId);

    List<RendezVous> findByStatutOrderByDateHeureDesc(StatutRdv statut);

    // Rappels : RDV dans 24h non encore rappelés
    @Query("SELECT r FROM RendezVous r WHERE r.statut = 'PLANIFIE' " +
           "AND r.dateHeure BETWEEN :debut AND :fin AND r.rappelEnvoye = false")
    List<RendezVous> findRdvForRappel(@Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin);

    // Stats pour le directeur
    @Query("SELECT r.statut, COUNT(r) FROM RendezVous r GROUP BY r.statut")
    List<Object[]> countByStatut();

    @Query("SELECT COUNT(r) FROM RendezVous r WHERE r.createdAt >= :depuis")
    Long countDepuis(@Param("depuis") LocalDateTime depuis);

    @Query("SELECT YEAR(r.dateHeure), MONTH(r.dateHeure), COUNT(r) FROM RendezVous r " +
           "WHERE r.dateHeure >= :depuis GROUP BY YEAR(r.dateHeure), MONTH(r.dateHeure) " +
           "ORDER BY YEAR(r.dateHeure), MONTH(r.dateHeure)")
    List<Object[]> countByMonth(@Param("depuis") LocalDateTime depuis);

    List<RendezVous> findAllByOrderByDateHeureDesc();
}
