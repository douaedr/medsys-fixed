package com.medsys.appointment.controller;

import com.medsys.appointment.entity.TimeSlot;
import com.medsys.appointment.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN')")
    public ResponseEntity<TimeSlot> create(@RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.create(req));
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeSlot>> getAllAvailable() {
        return ResponseEntity.ok(timeSlotService.getAllAvailable());
    }

    @GetMapping("/medecin/{medecinId}/available")
    public ResponseEntity<List<TimeSlot>> getByMedecin(@PathVariable Long medecinId) {
        return ResponseEntity.ok(timeSlotService.getAvailableByMedecin(medecinId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlot> getById(@PathVariable Long id) {
        return ResponseEntity.ok(timeSlotService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        timeSlotService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Créneau supprimé"));
    }
}
