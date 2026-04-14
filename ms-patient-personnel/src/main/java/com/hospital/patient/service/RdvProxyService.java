package com.hospital.patient.service;

import com.hospital.patient.dto.RendezVousDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
public class RdvProxyService {

    @Value("${ms-rdv.url:}")
    private String msRdvUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Récupère les rendez-vous d'un patient depuis ms-rdv.
     * Endpoint : GET /api/v1/rdv/appointments/patient/{patientId}
     */
    public List<RendezVousDTO> getRdvPatient(Long patientId) {
        if (msRdvUrl == null || msRdvUrl.isBlank()) {
            log.debug("ms-rdv.url non configuré, retour liste vide");
            return List.of();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(msRdvUrl)
                    .path("/api/v1/rdv/appointments/patient/{patientId}")
                    .buildAndExpand(patientId)
                    .toUriString();
            ResponseEntity<List<RendezVousDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Impossible de joindre ms-rdv getRdvPatient({}): {}", patientId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère tous les rendez-vous depuis ms-rdv (vue directeur).
     * Endpoint : GET /api/v1/rdv/appointments
     */
    public List<RendezVousDTO> getAllRdv() {
        if (msRdvUrl == null || msRdvUrl.isBlank()) return List.of();
        try {
            String url = msRdvUrl + "/api/v1/rdv/appointments";
            ResponseEntity<List<RendezVousDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Impossible de joindre ms-rdv getAllRdv: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Annule un rendez-vous via ms-rdv.
     * Endpoint : PATCH /api/v1/rdv/appointments/{id}/cancel?reason=...
     */
    public boolean annulerRdv(Long rdvId, Long patientId) {
        if (msRdvUrl == null || msRdvUrl.isBlank()) return false;
        try {
            String url = UriComponentsBuilder.fromHttpUrl(msRdvUrl)
                    .path("/api/v1/rdv/appointments/{rdvId}/cancel")
                    .queryParam("reason", "Annulé par le patient (id=" + patientId + ")")
                    .buildAndExpand(rdvId)
                    .toUriString();
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
            log.info("RDV {} annulé avec succès via ms-rdv", rdvId);
            return true;
        } catch (Exception e) {
            log.warn("Impossible d'annuler RDV {} via ms-rdv: {}", rdvId, e.getMessage());
            return false;
        }
    }
}
