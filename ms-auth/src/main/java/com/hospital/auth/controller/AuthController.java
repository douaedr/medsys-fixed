package com.hospital.auth.controller;

import com.hospital.auth.dto.*;
import com.hospital.auth.service.AuthService;
import com.hospital.auth.service.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!rateLimiter.isAllowed(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Trop de tentatives. Réessayez dans 1 minute."));
        }
        return ResponseEntity.ok(authService.login(req));
    }

    // POST /api/v1/auth/register  (Patient s'inscrit)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterPatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(req));
    }

    // POST /api/v1/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                                               HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!rateLimiter.isAllowed(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Trop de tentatives. Réessayez dans 1 minute."));
        }
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé si le compte existe."));
    }

    // POST /api/v1/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès."));
    }

    // POST /api/v1/auth/change-password  (nécessite auth)
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), req);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
    }

    // GET /api/v1/auth/verify?token=...
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyToken(token));
    }

    // GET /api/v1/auth/me  (infos du user connecté)
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("email", authentication.getName(), "authorities", authentication.getAuthorities()));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
