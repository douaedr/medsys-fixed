package com.medsys.billing.entity;

import com.medsys.billing.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod methode = PaymentMethod.CASH;

    private LocalDate datePaiement;
    private String reference;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
}
