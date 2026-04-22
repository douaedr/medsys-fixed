package com.medsys.appointment.entity;

import com.medsys.appointment.enums.AppointmentPriority;
import com.medsys.appointment.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long medecinId;
    private Long creneauId;

    @Enumerated(EnumType.STRING) @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Enumerated(EnumType.STRING) @Builder.Default
    private AppointmentPriority priority = AppointmentPriority.NORMAL;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(length = 500) private String motif;
    @Column(length = 1000) private String notes;

    private String patientNom;
    private String patientPrenom;
    private String medecinNom;
    private String medecinPrenom;
    private Long specialiteId;

    @Builder.Default private boolean rappelEnvoye = false;
    @Builder.Default private int noShowCount = 0;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
