package com.hospital.patient.controller;

import com.hospital.patient.security.JwtService;
import com.hospital.patient.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications temps réel via SSE")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Operation(
        summary = "S'abonner aux notifications SSE",
        description = "Ouvre un flux Server-Sent Events pour recevoir les notifications en temps réel"
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long patientId = jwtService.extractPatientId(token);
        if (patientId == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("Token invalide"));
            return emitter;
        }
        return notificationService.subscribe(patientId);
    }

    @Operation(summary = "Nombre de patients connectés (admin)")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "connectedPatients", notificationService.countConnectedPatients()
        ));
    }
}
