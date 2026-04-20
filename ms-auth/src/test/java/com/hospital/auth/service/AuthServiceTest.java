package com.hospital.auth.service;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterPatientRequest;
import com.hospital.auth.entity.UserAccount;
import com.hospital.auth.enums.Role;
import com.hospital.auth.exception.AuthException;
import com.hospital.auth.messaging.EventPublisher;
import com.hospital.auth.repository.AuditLogRepository;
import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserAccountRepository userRepo;
    @Mock private AuditLogRepository auditRepo;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private UserAccount activePatient;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "msPatientUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);

        activePatient = UserAccount.builder()
                .id(1L)
                .email("patient@medsys.ma")
                .password("$2a$10$encoded")
                .role(Role.PATIENT)
                .nom("Alami")
                .prenom("Fatima")
                .enabled(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .patientId(10L)
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() - credentials valides → token retourné")
    void login_validCredentials_returnsToken() {
        when(userRepo.findByEmail("patient@medsys.ma")).thenReturn(Optional.of(activePatient));
        when(passwordEncoder.matches("Pass1234!", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generateToken(activePatient)).thenReturn("jwt.token.here");
        when(userRepo.save(any())).thenReturn(activePatient);
        doNothing().when(eventPublisher).publishUserLoggedIn(any());

        LoginRequest req = new LoginRequest();
        req.setEmail("patient@medsys.ma");
        req.setPassword("Pass1234!");

        var response = authService.login(req, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getRole()).isEqualTo("PATIENT");
    }

    @Test
    @DisplayName("login() - email inexistant → AuthException")
    void login_unknownEmail_throwsAuthException() {
        when(userRepo.findByEmail("unknown@medsys.ma")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@medsys.ma");
        req.setPassword("any");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("login() - mot de passe incorrect → AuthException + tentative incrémentée")
    void login_wrongPassword_incrementsFailedAttempts() {
        when(userRepo.findByEmail("patient@medsys.ma")).thenReturn(Optional.of(activePatient));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(userRepo.save(any())).thenReturn(activePatient);

        LoginRequest req = new LoginRequest();
        req.setEmail("patient@medsys.ma");
        req.setPassword("WrongPass!");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("mot de passe");

        assertThat(activePatient.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("login() - compte désactivé → AuthException 403")
    void login_disabledAccount_throwsForbidden() {
        activePatient.setEnabled(false);
        when(userRepo.findByEmail("patient@medsys.ma")).thenReturn(Optional.of(activePatient));

        LoginRequest req = new LoginRequest();
        req.setEmail("patient@medsys.ma");
        req.setPassword("Pass1234!");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    @DisplayName("login() - email non vérifié → AuthException")
    void login_emailNotVerified_throwsAuthException() {
        activePatient.setEmailVerified(false);
        when(userRepo.findByEmail("patient@medsys.ma")).thenReturn(Optional.of(activePatient));

        LoginRequest req = new LoginRequest();
        req.setEmail("patient@medsys.ma");
        req.setPassword("Pass1234!");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("vérifier votre adresse email");
    }

    // ── Verify token ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyToken() - token valide → valid=true + claims")
    void verifyToken_valid() {
        when(jwtService.isTokenValid("valid.jwt")).thenReturn(true);
        when(jwtService.extractEmail("valid.jwt")).thenReturn("admin@medsys.ma");
        when(jwtService.extractRole("valid.jwt")).thenReturn("ADMIN");
        when(jwtService.extractUserId("valid.jwt")).thenReturn(99L);

        var result = authService.verifyToken("valid.jwt");

        assertThat(result).containsEntry("valid", true);
        assertThat(result).containsEntry("email", "admin@medsys.ma");
        assertThat(result).containsEntry("role", "ADMIN");
    }

    @Test
    @DisplayName("verifyToken() - token invalide → valid=false")
    void verifyToken_invalid() {
        when(jwtService.isTokenValid("bad.token")).thenReturn(false);

        var result = authService.verifyToken("bad.token");

        assertThat(result).containsEntry("valid", false);
        assertThat(result).doesNotContainKey("email");
    }

    // ── Verify email ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyEmail() - token valide → email marqué vérifié")
    void verifyEmail_success() {
        activePatient.setEmailVerified(false);
        activePatient.setEmailVerificationToken("verify-token-abc");

        when(userRepo.findByEmailVerificationToken("verify-token-abc"))
                .thenReturn(Optional.of(activePatient));
        when(userRepo.save(any())).thenReturn(activePatient);

        authService.verifyEmail("verify-token-abc");

        assertThat(activePatient.isEmailVerified()).isTrue();
        assertThat(activePatient.getEmailVerificationToken()).isNull();
    }

    @Test
    @DisplayName("verifyEmail() - token invalide → AuthException")
    void verifyEmail_invalidToken() {
        when(userRepo.findByEmailVerificationToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("invalide");
    }
}
