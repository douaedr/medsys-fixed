package com.medsys.billing.controller;

import com.medsys.billing.entity.Invoice;
import com.medsys.billing.enums.InvoiceStatus;
import com.medsys.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('MEDECIN','SECRETAIRE','ADMIN')")
    public ResponseEntity<Invoice> createInvoice(@RequestBody Map<String, Object> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createInvoice(req));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN','DIRECTEUR')")
    public ResponseEntity<List<Invoice>> getAll() {
        return ResponseEntity.ok(billingService.getAll());
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN','DIRECTEUR','PATIENT','MEDECIN')")
    public ResponseEntity<Invoice> getById(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getById(id));
    }

    @GetMapping("/invoices/patient/{patientId}")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN','DIRECTEUR','PATIENT','MEDECIN')")
    public ResponseEntity<List<Invoice>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(billingService.getByPatient(patientId));
    }

    @GetMapping("/invoices/status/{status}")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN','DIRECTEUR')")
    public ResponseEntity<List<Invoice>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(billingService.getByStatus(InvoiceStatus.valueOf(status.toUpperCase())));
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN')")
    public ResponseEntity<Invoice> addPayment(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(billingService.addPayment(id, req));
    }

    @PutMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN')")
    public ResponseEntity<Invoice> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.cancel(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(billingService.getStats());
    }
}
