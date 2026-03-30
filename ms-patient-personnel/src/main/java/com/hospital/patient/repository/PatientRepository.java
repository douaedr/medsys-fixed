package com.hospital.patient.repository;

import com.hospital.patient.entity.Patient;
import com.hospital.patient.enums.GroupeSanguin;
import com.hospital.patient.enums.Sexe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByCin(String cin);

    Boolean existsByCin(String cin);

    List<Patient> findByVilleIgnoreCase(String ville);

    List<Patient> findByGroupeSanguin(GroupeSanguin groupeSanguin);

    List<Patient> findBySexe(Sexe sexe);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "p.cin LIKE CONCAT('%', :search, '%')")
    Page<Patient> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.createdAt >= :date")
    Long countPatientsCreatedAfter(@Param("date") LocalDateTime date);

    @Query("SELECT p.ville, COUNT(p) FROM Patient p WHERE p.ville IS NOT NULL AND p.ville != '' GROUP BY p.ville ORDER BY COUNT(p) DESC")
    List<Object[]> countByVille();

    @Query("SELECT p.groupeSanguin, COUNT(p) FROM Patient p WHERE p.groupeSanguin IS NOT NULL GROUP BY p.groupeSanguin ORDER BY COUNT(p) DESC")
    List<Object[]> countByGroupeSanguin();

    @Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), COUNT(p) FROM Patient p WHERE p.createdAt >= :since GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) ORDER BY YEAR(p.createdAt), MONTH(p.createdAt)")
    List<Object[]> countByMonth(@Param("since") LocalDateTime since);

    // Recherche avancée multi-critères
    @Query("SELECT p FROM Patient p WHERE " +
           "(:nom IS NULL OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
           "(:prenom IS NULL OR LOWER(p.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
           "(:ville IS NULL OR LOWER(p.ville) = LOWER(:ville)) AND " +
           "(:sexe IS NULL OR p.sexe = :sexe) AND " +
           "(:groupeSanguin IS NULL OR p.groupeSanguin = :groupeSanguin) AND " +
           "(:ageMin IS NULL OR (YEAR(CURRENT_DATE) - YEAR(p.dateNaissance)) >= :ageMin) AND " +
           "(:ageMax IS NULL OR (YEAR(CURRENT_DATE) - YEAR(p.dateNaissance)) <= :ageMax)")
    Page<Patient> rechercheAvancee(
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("ville") String ville,
            @Param("sexe") Sexe sexe,
            @Param("groupeSanguin") GroupeSanguin groupeSanguin,
            @Param("ageMin") Integer ageMin,
            @Param("ageMax") Integer ageMax,
            Pageable pageable);
}
