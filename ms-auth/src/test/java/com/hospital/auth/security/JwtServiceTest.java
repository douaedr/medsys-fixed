package com.hospital.auth.security;

import com.hospital.auth.entity.UserAccount;
import com.hospital.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "medsys-hospital-jwt-secret-key-2026-very-long-and-secure-string-please-change-in-prod";

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    @DisplayName("generateToken() puis extractEmail() → même email")
    void generateAndExtractEmail() {
        UserAccount user = UserAccount.builder()
                .id(1L).email("admin@medsys.ma").role(Role.ADMIN)
                .nom("Admin").prenom("Test").build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@medsys.ma");
    }

    @Test
    @DisplayName("generateToken() puis extractRole() → bon rôle")
    void generateAndExtractRole() {
        UserAccount user = UserAccount.builder()
                .id(2L).email("medecin@medsys.ma").role(Role.MEDECIN)
                .nom("Hassan").prenom("Dr").build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractRole(token)).isEqualTo("MEDECIN");
    }

    @Test
    @DisplayName("isTokenValid() → true pour token frais")
    void isTokenValid_freshToken_returnsTrue() {
        UserAccount user = UserAccount.builder()
                .id(3L).email("patient@medsys.ma").role(Role.PATIENT)
                .nom("Benali").prenom("Omar").patientId(42L).build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() → false pour token expiré")
    void isTokenValid_expiredToken_returnsFalse() {
        JwtService shortLived = new JwtService();
        ReflectionTestUtils.setField(shortLived, "secretKey", SECRET);
        ReflectionTestUtils.setField(shortLived, "jwtExpiration", -1000L);

        UserAccount user = UserAccount.builder()
                .id(4L).email("expired@medsys.ma").role(Role.PATIENT)
                .nom("Old").prenom("User").build();

        String expiredToken = shortLived.generateToken(user);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() → false pour token malformé")
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("extractUserId() → retourne l'ID utilisateur")
    void extractUserId() {
        UserAccount user = UserAccount.builder()
                .id(99L).email("user@medsys.ma").role(Role.DIRECTEUR)
                .nom("Directeur").prenom("Test").build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(99L);
    }
}
