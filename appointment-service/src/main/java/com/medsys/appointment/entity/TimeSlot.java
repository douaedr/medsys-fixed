package com.medsys.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_slots")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long medecinId;
    @Column(nullable = false) private LocalDateTime debut;
    @Column(nullable = false) private LocalDateTime fin;

    @Builder.Default private boolean disponible = true;
    @Builder.Default private int dureeMinutes = 30;

    private String type;  // CONSULTATION, FOLLOW_UP, EMERGENCY
    private Long specialiteId;
}
