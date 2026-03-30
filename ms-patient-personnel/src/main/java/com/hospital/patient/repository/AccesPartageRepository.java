package com.hospital.patient.repository;

import com.hospital.patient.entity.AccesPartage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccesPartageRepository extends JpaRepository<AccesPartage, Long> {

    Optional<AccesPartage> findByToken(String token);

    List<AccesPartage> findByCreePar(String email);

    @Modifying
    @Query("UPDATE AccesPartage a SET a.actif = false WHERE a.expireAt < :now AND a.actif = true")
    int desactiverExpires(@Param("now") LocalDateTime now);
}
