package com.hospital.patient.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement RabbitMQ reçu de ms-rdv via medsys.exchange.
 *
 * <p>Les noms de champs correspondent exactement au payload publié par
 * {@code AppointmentService.publishEvent()} dans ms-rdv.</p>
 *
 * <p>Champs obligatoires : eventType, appointmentId, patientId, medecinId, dateHeure.</p>
 * <p>Champs optionnels  : noms, motif, notes, noShowCount, timestamp.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEvent {

    @JsonProperty("eventType")
    private String eventType;        // APPOINTMENT_CREATED | APPOINTMENT_CANCELLED | APPOINTMENT_NOSHOW

    @JsonProperty("appointmentId")
    private Long appointmentId;

    @JsonProperty("patientId")
    private Long patientId;

    /** ID du médecin tel que publié par ms-rdv (champ "medecinId"). */
    @JsonProperty("medecinId")
    private Long medecinId;

    @JsonProperty("medecinNom")
    private String medecinNom;

    @JsonProperty("medecinPrenom")
    private String medecinPrenom;

    @JsonProperty("patientNom")
    private String patientNom;

    @JsonProperty("patientPrenom")
    private String patientPrenom;

    /** Date/heure au format ISO-8601 (ex: "2026-04-10T14:30:00"). */
    @JsonProperty("dateHeure")
    private String dateHeure;

    @JsonProperty("status")
    private String status;

    @JsonProperty("motif")
    private String motif;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("noShowCount")
    private int noShowCount;

    @JsonProperty("timestamp")
    private String timestamp;
}
