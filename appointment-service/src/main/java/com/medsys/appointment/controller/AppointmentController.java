package com.medsys.appointment.controller;

import com.medsys.appointment.entity.Appointment;
import com.medsys.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','PATIENT')")
    public ResponseEntity<Appointment> create(@RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Appointment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','DIRECTEUR','PATIENT')")
    public ResponseEntity<List<Appointment>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getByPatientId(patientId));
    }

    @GetMapping("/medecin/{medecinId}")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','DIRECTEUR')")
    public ResponseEntity<List<Appointment>> getByMedecin(@PathVariable Long medecinId) {
        return ResponseEntity.ok(appointmentService.getByMedecinId(medecinId));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN')")
    public ResponseEntity<Appointment> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.confirm(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Appointment> cancel(@PathVariable Long id,
                                               @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(appointmentService.cancel(id, reason));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<Appointment> complete(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.complete(id));
    }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN')")
    public ResponseEntity<Appointment> markNoShow(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.markNoShow(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR','MEDECIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(appointmentService.getStats());
    }
}
