package com.hospital.patient.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RendezVousRequestDTO {

    @NotNull(message = "La date/heure est obligatoire")
    @Future(message = "Le rendez-vous doit être dans le futur")
    private LocalDateTime dateHeure;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 500)
    private String motif;

    private Long medecinId;

    @Size(max = 100)
    private String service;

    @Size(max = 200)
    private String lieu;

    @Size(max = 1000)
    private String notes;
}
