package com.hospital.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterPatientRequest;
import com.hospital.auth.exception.AuthException;
import com.hospital.auth.service.AuthService;
import com.hospital.auth.service.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private LoginRateLimiter rateLimiter;

    @MockBean
    private com.hospital.auth.security.JwtAuthFilter jwtAuthFilter;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setup() {
        mockAuthResponse = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test")
                .refreshToken("refresh-token-123")
                .type("Bearer")
                .userId(1L)
                .email("patient@medsys.ma")
                .nom("Alami")
                .prenom("Fatima")
                .role("PATIENT")
                .patientId(10L)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - succès")
    void login_success() throws Exception {
        when(rateLimiter.isAllowed(anyString())).thenReturn(true);
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(mockAuthResponse);

        LoginRequest req = new LoginRequest();
        req.setEmail("patient@medsys.ma");
        req.setPassword("Patient1234!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.email").value("patient@medsys.ma"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - trop de tentatives → 429")
    void login_tooManyRequests() throws Exception {
        when(rateLimiter.isAllowed(anyString())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("brute@force.com");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - mauvais mot de passe → 401")
    void login_badCredentials() throws Exception {
        when(rateLimiter.isAllowed(anyString())).thenReturn(true);
        when(authService.login(any(), anyString()))
                .thenThrow(new AuthException("Email ou mot de passe incorrect"));

        LoginRequest req = new LoginRequest();
        req.setEmail("patient@medsys.ma");
        req.setPassword("WrongPass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - inscription patient")
    void register_success() throws Exception {
        when(authService.registerPatient(any(RegisterPatientRequest.class)))
                .thenReturn(mockAuthResponse);

        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setEmail("nouveau@medsys.ma");
        req.setPassword("Pass1234!");
        req.setNom("Benali");
        req.setPrenom("Youssef");
        req.setCin("AB123456");
        req.setDateNaissance(LocalDate.of(1990, 5, 15));
        req.setSexe("MASCULIN");
        req.setGroupeSanguin("A_POSITIF");
        req.setTelephone("0612345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/verify - token valide")
    void verifyToken_valid() throws Exception {
        when(authService.verifyToken("valid-token"))
                .thenReturn(java.util.Map.of("valid", true, "email", "patient@medsys.ma", "role", "PATIENT"));

        mockMvc.perform(get("/api/v1/auth/verify").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - utilisateur authentifié")
    @WithMockUser(username = "admin@medsys.ma", roles = "ADMIN")
    void me_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@medsys.ma"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - non authentifié → 401")
    void me_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
