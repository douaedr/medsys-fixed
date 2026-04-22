package com.medsys.auth.controller;

import com.medsys.auth.dto.AuthResponse;
import com.medsys.auth.dto.CreatePersonnelRequest;
import com.medsys.auth.entity.UserAccount;
import com.medsys.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    @PostMapping("/personnel")
    public ResponseEntity<AuthResponse> createPersonnel(@Valid @RequestBody CreatePersonnelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createPersonnelAccount(req));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserAccount>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserAccount>> listByRole(@PathVariable String role) {
        return ResponseEntity.ok(
                authService.listUsers().stream()
                        .filter(u -> u.getRole().name().equalsIgnoreCase(role))
                        .toList()
        );
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<UserAccount> toggleUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.toggleUser(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }
}
