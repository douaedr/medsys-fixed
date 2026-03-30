package com.hospital.auth.service;

import com.hospital.auth.dto.ChangePasswordRequest;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.entity.UserAccount;
import com.hospital.auth.enums.Role;
import com.hospital.auth.exception.AuthException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AuthService")
class AuthServiceTest {

    @Mock private UserAccountRepository userRepo;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    private UserAccount activeUser;

    @BeforeEach
    void setup() {
        activeUser = UserAccount.builder()
                .id(1L)
                .email("test@medsys.ma")
                .password("hashed_password")
                .nom("Dupont")
                .prenom("Jean")
                .role(Role.PATIENT)
                .enabled(true)
                .build();
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login réussi avec email/password corrects")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@medsys.ma");
        req.setPassword("password123");

        when(userRepo.findByEmail("test@medsys.ma")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("jwt_token");

        AuthResponse response = authService.login(req);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token");
        assertThat(response.getEmail()).isEqualTo("test@medsys.ma");
        assertThat(response.getRole()).isEqualTo("PATIENT");
    }

    @Test
    @DisplayName("Login échoue si email introuvable")
    void login_emailNotFound_throwsAuthException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("inconnu@test.ma");
        req.setPassword("password123");

        when(userRepo.findByEmail("inconnu@test.ma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("Login échoue si mot de passe incorrect")
    void login_wrongPassword_throwsAuthException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@medsys.ma");
        req.setPassword("mauvais");

        when(userRepo.findByEmail("test@medsys.ma")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("mauvais", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("Login échoue si compte désactivé")
    void login_disabledAccount_throwsAuthException() {
        activeUser.setEnabled(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("test@medsys.ma");
        req.setPassword("password123");

        when(userRepo.findByEmail("test@medsys.ma")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Compte désactivé");
    }

    // ── Change Password ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Changement de mot de passe réussi")
    void changePassword_success() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("ancienMdp");
        req.setNewPassword("nouveauMdp");

        when(userRepo.findByEmail("test@medsys.ma")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("ancienMdp", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("nouveauMdp")).thenReturn("nouveau_hashed");

        authService.changePassword("test@medsys.ma", req);

        verify(userRepo).save(argThat(u -> u.getPassword().equals("nouveau_hashed")));
    }

    @Test
    @DisplayName("Changement de mot de passe échoue si ancien MDP incorrect")
    void changePassword_wrongOldPassword_throwsAuthException() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("mauvais");
        req.setNewPassword("nouveauMdp");

        when(userRepo.findByEmail("test@medsys.ma")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("mauvais", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("test@medsys.ma", req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Ancien mot de passe incorrect");
    }

    // ── Verify Token ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyToken retourne valid=true pour un token valide")
    void verifyToken_valid() {
        when(jwtService.isTokenValid("valid_token")).thenReturn(true);
        when(jwtService.extractEmail("valid_token")).thenReturn("test@medsys.ma");
        when(jwtService.extractRole("valid_token")).thenReturn("PATIENT");
        when(jwtService.extractUserId("valid_token")).thenReturn(1L);

        var result = authService.verifyToken("valid_token");

        assertThat(result).containsEntry("valid", true);
        assertThat(result).containsEntry("email", "test@medsys.ma");
    }

    @Test
    @DisplayName("verifyToken retourne valid=false pour un token invalide")
    void verifyToken_invalid() {
        when(jwtService.isTokenValid("bad_token")).thenReturn(false);

        var result = authService.verifyToken("bad_token");

        assertThat(result).containsEntry("valid", false);
        assertThat(result).doesNotContainKey("email");
    }
}
