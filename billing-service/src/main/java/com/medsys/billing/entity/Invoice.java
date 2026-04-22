package com.medsys.billing.entity;

import com.medsys.billing.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Invoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroFacture;

    @Column(nullable = false)
    private Long patientId;

    private String patientNom;
    private String patientPrenom;
    private String patientCin;

    private Long appointmentId;
    private Long medecinId;
    private String medecinNom;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal remise = BigDecimal.ZERO;

    private LocalDate dateFacture;
    private LocalDate dateEcheance;

    @Column(length = 500)
    private String notes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "invoice")
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "invoice")
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void generateNumeroFacture() {
        if (numeroFacture == null) {
            numeroFacture = "FAC-" + LocalDate.now().getYear() + "-"
                    + String.format("%06d", System.currentTimeMillis() % 1_000_000);
        }
        if (dateFacture == null) dateFacture = LocalDate.now();
        if (dateEcheance == null) dateEcheance = LocalDate.now().plusDays(30);
    }
}
