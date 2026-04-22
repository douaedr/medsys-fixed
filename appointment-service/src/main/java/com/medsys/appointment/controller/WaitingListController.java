package com.medsys.appointment.controller;

import com.medsys.appointment.entity.WaitingListEntry;
import com.medsys.appointment.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/waiting-list")
@RequiredArgsConstructor
public class WaitingListController {

    private final WaitingListService waitingListService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','PATIENT')")
    public ResponseEntity<WaitingListEntry> addToList(@RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitingListService.addToWaitingList(req));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN','PATIENT')")
    public ResponseEntity<List<WaitingListEntry>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(waitingListService.getByPatient(patientId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN')")
    public ResponseEntity<List<WaitingListEntry>> getPending() {
        return ResponseEntity.ok(waitingListService.getPendingList());
    }
}
