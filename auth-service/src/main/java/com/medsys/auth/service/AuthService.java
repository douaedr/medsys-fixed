package com.medsys.auth.service;

import com.medsys.auth.dto.*;
import com.medsys.auth.entity.AuditLog;
import com.medsys.auth.entity.UserAccount;
import com.medsys.auth.enums.Role;
import com.medsys.auth.exception.AuthException;
import com.medsys.auth.messaging.EventPublisher;
import com.medsys.auth.repository.AuditLogRepository;
import com.medsys.auth.repository.UserAccountRepository;
import com.medsys.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserAccountRepository userRepo;
    private final AuditLogRepository auditRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EventPublisher eventPublisher;

    @Value("${app.ms-patient-url}")
    private String msPatientUrl;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    public AuthResponse login(LoginRequest req, String ip) {
        UserAccount user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    audit("LOGIN_FAILURE", req.getEmail(), ip, "Email non trouvé");
                    return new AuthException("Email ou mot de passe incorrect");
                });

        if (!user.isEnabled()) {
            audit("LOGIN_FAILURE", req.getEmail(), ip, "Compte désactivé");
            throw new AuthException("Votre compte est désactivé. Contactez l'administrateur.",
                    HttpStatus.FORBIDDEN);
        }

        if (user.isAccountLocked()) {
            audit("LOGIN_FAILURE", req.getEmail(), ip, "Compte verrouillé");
            throw new AuthException(
                    "Compte temporairement verrouillé. Réessayez dans " + LOCK_DURATION_MINUTES + " minutes.",
                    HttpStatus.FORBIDDEN);
        }

        if (!user.isEmailVerified()) {
            audit("LOGIN_FAILURE", req.getEmail(), ip, "Email non vérifié");
            throw new AuthException("Veuillez vérifier votre adresse email avant de vous connecter.",
                    HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ip);
            throw new AuthException("Email ou mot de passe incorrect");
        }

        user.resetFailedAttempts();
        issueRefreshToken(user);
        userRepo.save(user);

        audit("LOGIN_SUCCESS", user.getEmail(), ip, "role=" + user.getRole());
        eventPublisher.publishUserLoggedIn(user);
        return buildAuthResponse(user);
    }

    public AuthResponse registerPatient(RegisterPatientRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AuthException("Un compte avec cet email existe déjà");
        if (userRepo.existsByCin(req.getCin()))
            throw new AuthException("Un compte avec ce CIN existe déjà");

        Long patientId = createPatientInPatientService(req);
        String verificationToken = UUID.randomUUID().toString();

        UserAccount user = UserAccount.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.PATIENT)
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .cin(req.getCin().toUpperCase())
                .patientId(patientId)
                .enabled(true)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .build();

        userRepo.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getNom(), verificationToken);

        audit("REGISTRATION", user.getEmail(), null,
                "Patient enregistré: " + user.getNom() + " " + user.getPrenom());
        eventPublisher.publishUserCreated(user);
        return buildAuthResponse(user);
    }

    public void verifyEmail(String token) {
        UserAccount user = userRepo.findByEmailVerificationToken(token)
                .orElseThrow(() -> new AuthException("Token de vérification invalide ou déjà utilisé"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepo.save(user);
        audit("EMAIL_VERIFIED", user.getEmail(), null, "Email vérifié");
    }

    public AuthResponse refreshToken(String refreshToken) {
        UserAccount user = userRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthException("Refresh token invalide ou expiré"));

        if (user.getRefreshTokenExpiry() == null
                || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepo.save(user);
            throw new AuthException("Refresh token expiré. Veuillez vous reconnecter.");
        }

        issueRefreshToken(user);
        userRepo.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse createPersonnelAccount(CreatePersonnelRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AuthException("Un compte avec cet email existe déjà");

        UserAccount user = UserAccount.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .cin(req.getCin())
                .personnelId(req.getPersonnelId())
                .enabled(true)
                .emailVerified(true)
                .build();

        userRepo.save(user);
        emailService.sendAccountCreatedEmail(user.getEmail(), user.getNom(), req.getPassword());
        eventPublisher.publishUserCreated(user);
        audit("REGISTRATION", user.getEmail(), null, "Compte personnel créé, role=" + req.getRole());
        return buildAuthResponse(user);
    }

    public void forgotPassword(ForgotPasswordRequest req) {
        userRepo.findByEmail(req.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepo.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getNom(), token);
            audit("PASSWORD_RESET_REQUEST", user.getEmail(), null, "Token de réinitialisation généré");
        });
    }

    public void resetPassword(ResetPasswordRequest req) {
        UserAccount user = userRepo.findByResetToken(req.getToken())
                .orElseThrow(() -> new AuthException("Token invalide ou expiré"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            throw new AuthException("Token expiré. Veuillez refaire la demande.");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.resetFailedAttempts();
        userRepo.save(user);
        audit("PASSWORD_RESET", user.getEmail(), null, "Mot de passe réinitialisé");
    }

    public void changePassword(String email, ChangePasswordRequest req) {
        UserAccount user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AuthException("Utilisateur non trouvé"));
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword()))
            throw new AuthException("Ancien mot de passe incorrect");
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
        audit("PASSWORD_CHANGED", email, null, "Mot de passe modifié");
    }

    public Map<String, Object> verifyToken(String token) {
        Map<String, Object> result = new HashMap<>();
        boolean valid = jwtService.isTokenValid(token);
        result.put("valid", valid);
        if (valid) {
            result.put("email", jwtService.extractEmail(token));
            result.put("role", jwtService.extractRole(token));
            result.put("userId", jwtService.extractUserId(token));
        }
        return result;
    }

    public UserAccount getProfile(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AuthException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
    }

    // ── Admin methods ──────────────────────────────────────────────────────
    public List<UserAccount> listUsers() {
        return userRepo.findAll();
    }

    public UserAccount toggleUser(Long id) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new AuthException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        user.setEnabled(!user.isEnabled());
        return userRepo.save(user);
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void handleFailedLogin(UserAccount user, String ip) {
        user.incrementFailedAttempts();
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount(LOCK_DURATION_MINUTES);
            audit("ACCOUNT_LOCKED", user.getEmail(), ip,
                    "Verrouillé après " + MAX_FAILED_ATTEMPTS + " tentatives");
        } else {
            audit("LOGIN_FAILURE", user.getEmail(), ip,
                    "Mauvais mot de passe, tentative=" + user.getFailedLoginAttempts());
        }
        userRepo.save(user);
    }

    private void issueRefreshToken(UserAccount user) {
        user.setRefreshToken(UUID.randomUUID().toString());
        user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
    }

    private AuthResponse buildAuthResponse(UserAccount user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .refreshToken(user.getRefreshToken())
                .userId(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole().name())
                .patientId(user.getPatientId())
                .personnelId(user.getPersonnelId())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private void audit(String eventType, String email, String ip, String details) {
        try {
            auditRepo.save(AuditLog.builder()
                    .eventType(eventType).email(email).ipAddress(ip).details(details).build());
        } catch (Exception e) {
            log.warn("[AUDIT] Échec persistance: {}", e.getMessage());
        }
    }

    private Long createPatientInPatientService(RegisterPatientRequest req) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("nom", req.getNom());
            body.put("prenom", req.getPrenom());
            body.put("cin", req.getCin().toUpperCase());
            body.put("dateNaissance", req.getDateNaissance().toString());
            body.put("sexe", req.getSexe());
            body.put("groupeSanguin", req.getGroupeSanguin());
            body.put("telephone", req.getTelephone());
            body.put("email", req.getEmail());
            body.put("adresse", req.getAdresse());
            body.put("ville", req.getVille());
            body.put("mutuelle", req.getMutuelle());
            body.put("numeroCNSS", req.getNumeroCNSS());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    msPatientUrl + "/api/v1/patients",
                    new HttpEntity<>(body, headers),
                    Map.class);

            if (response.getBody() != null && response.getBody().get("id") != null)
                return Long.valueOf(response.getBody().get("id").toString());
        } catch (Exception e) {
            log.warn("[PATIENT-SERVICE] Indisponible, patient créé sans lien: {}", e.getMessage());
        }
        return null;
    }
}
