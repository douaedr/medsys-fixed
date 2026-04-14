package com.hospital.patient.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message reçu depuis auth.exchange lorsque ms-auth publie un événement
 * USER_CREATED ou USER_LOGGED_IN.
 *
 * <p>Doit correspondre exactement à {@code AuthEventMessage} de ms-auth.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthEventMessage {

    @JsonProperty("eventType")
    private String eventType;       // USER_CREATED | USER_LOGGED_IN

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private String role;            // PATIENT | MEDECIN | ADMIN | ...

    @JsonProperty("nom")
    private String nom;

    @JsonProperty("prenom")
    private String prenom;

    @JsonProperty("patientId")
    private Long patientId;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
