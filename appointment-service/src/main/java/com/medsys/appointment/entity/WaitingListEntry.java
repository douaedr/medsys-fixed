package com.medsys.appointment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "waiting_list_entries")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WaitingListEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long medecinId;
    private Long specialiteId;
    private String motif;
    private String priority;  // NORMAL, HIGH, URGENT

    @Builder.Default private boolean processed = false;
    private Long assignedAppointmentId;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
}
