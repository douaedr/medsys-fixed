package com.hospital.patient.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilRequest {

    @Pattern(regexp = "^[+]?[0-9\\s\\-]{6,20}$", message = "Format téléphone invalide")
    private String telephone;

    @Email(message = "Format email invalide")
    @Size(max = 150)
    private String email;

    @Size(max = 255)
    private String adresse;

    @Size(max = 100)
    private String ville;

    @Size(max = 100)
    private String mutuelle;

    @Size(max = 50)
    private String numeroCNSS;
}
