package com.hospital.auth.security;

import com.hospital.auth.entity.UserAccount;
import com.hospital.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private UserAccount user;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
            "medsys-test-jwt-secret-key-very-long-and-secure-at-least-32-chars");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        user = UserAccount.builder()
                .id(1L)
                .email("patient@medsys.ma")
                .nom("Alami")
                .prenom("Hassan")
                .role(Role.PATIENT)
                .patientId(42L)
                .build();
    }

    @Test
    @DisplayName("generateToken crée un token non null")
    void generateToken_notNull() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractEmail retourne l'email du token")
    void extractEmail_correct() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractEmail(token)).isEqualTo("patient@medsys.ma");
    }

    @Test
    @DisplayName("extractRole retourne le role du token")
    void extractRole_correct() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractRole(token)).isEqualTo("PATIENT");
    }

    @Test
    @DisplayName("extractUserId retourne l'id utilisateur")
    void extractUserId_correct() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("isTokenValid retourne true pour un token valide")
    void isTokenValid_true() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid retourne false pour un token invalide")
    void isTokenValid_false() {
        assertThat(jwtService.isTokenValid("ceci.nest.pasuntoken")).isFalse();
    }
}
