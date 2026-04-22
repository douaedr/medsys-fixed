package com.medsys.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    private String code;
    private String categorie; // CONSULTATION, ANALYSE, RADIOLOGIE, MEDICAMENT, ACTE, AUTRE

    @Builder.Default
    private int quantite = 1;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal prixUnitaire;

    @Column(precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (prixUnitaire != null) {
            prixTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        }
    }
}
