package com.hospital.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.patient.dto.PatientRequestDTO;
import com.hospital.patient.dto.PatientResponseDTO;
import com.hospital.patient.enums.GroupeSanguin;
import com.hospital.patient.enums.Sexe;
import com.hospital.patient.service.PatientDashboardService;
import com.hospital.patient.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    @MockBean
    private PatientDashboardService dashboardService;

    @MockBean
    private com.hospital.patient.security.JwtAuthFilter jwtAuthFilter;

    private PatientResponseDTO samplePatient;

    @BeforeEach
    void setup() {
        samplePatient = PatientResponseDTO.builder()
                .id(1L)
                .nom("Alami")
                .prenom("Fatima")
                .cin("AB123456")
                .dateNaissance(LocalDate.of(1985, 3, 20))
                .sexe(Sexe.FEMININ)
                .groupeSanguin(GroupeSanguin.A_POSITIF)
                .telephone("0612345678")
                .email("fatima@medsys.ma")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/patients - liste paginée (rôle MEDECIN)")
    @WithMockUser(roles = "MEDECIN")
    void getAllPatients_success() throws Exception {
        Page<PatientResponseDTO> page = new PageImpl<>(List.of(samplePatient));
        when(patientService.getAllPatients(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nom").value("Alami"))
                .andExpect(jsonPath("$.content[0].cin").value("AB123456"));
    }

    @Test
    @DisplayName("GET /api/v1/patients - non authentifié → 401")
    void getAllPatients_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/patients/{id} - patient trouvé")
    @WithMockUser(roles = "MEDECIN")
    void getById_found() throws Exception {
        when(patientService.getPatientById(1L)).thenReturn(samplePatient);

        mockMvc.perform(get("/api/v1/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Alami"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/patients - création patient (rôle ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void createPatient_success() throws Exception {
        when(patientService.createPatient(any(PatientRequestDTO.class))).thenReturn(samplePatient);

        PatientRequestDTO req = PatientRequestDTO.builder()
                .nom("Alami")
                .prenom("Fatima")
                .cin("AB123456")
                .dateNaissance(LocalDate.of(1985, 3, 20))
                .sexe(Sexe.FEMININ)
                .groupeSanguin(GroupeSanguin.A_POSITIF)
                .build();

        mockMvc.perform(post("/api/v1/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cin").value("AB123456"));
    }

    @Test
    @DisplayName("POST /api/v1/patients - body invalide (nom manquant) → 400")
    @WithMockUser(roles = "ADMIN")
    void createPatient_invalidBody() throws Exception {
        PatientRequestDTO invalid = new PatientRequestDTO();
        // nom, prenom, cin manquants → validation @NotBlank doit échouer

        mockMvc.perform(post("/api/v1/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/patients/{id} - suppression réussie")
    @WithMockUser(roles = "ADMIN")
    void deletePatient_success() throws Exception {
        mockMvc.perform(delete("/api/v1/patients/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/patients/statistiques - stats globales")
    @WithMockUser(roles = "DIRECTEUR")
    void getStatistiques() throws Exception {
        when(patientService.getStatistiques()).thenReturn(Map.of("total", 150L, "hommes", 70L, "femmes", 80L));

        mockMvc.perform(get("/api/v1/patients/statistiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(150));
    }

    @Test
    @DisplayName("GET /api/v1/patients/search - recherche par nom")
    @WithMockUser(roles = "MEDECIN")
    void searchPatients() throws Exception {
        Page<PatientResponseDTO> page = new PageImpl<>(List.of(samplePatient));
        when(patientService.searchPatients(eq("Alami"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/patients/search").param("q", "Alami"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nom").value("Alami"));
    }
}
