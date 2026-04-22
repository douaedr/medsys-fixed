package com.medsys.billing.repository;

import com.medsys.billing.entity.Invoice;
import com.medsys.billing.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatientId(Long patientId);
    List<Invoice> findByStatus(InvoiceStatus status);
    Optional<Invoice> findByNumeroFacture(String numero);
    long countByStatus(InvoiceStatus status);
    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COALESCE(SUM(i.montantPaye), 0) FROM Invoice i WHERE i.status = 'PAID'")
    BigDecimal sumRevenuTotal();

    @Query("SELECT COALESCE(SUM(i.montantTotal - i.montantPaye), 0) FROM Invoice i WHERE i.status = 'PENDING' OR i.status = 'PARTIALLY_PAID'")
    BigDecimal sumMontantEnAttente();
}
