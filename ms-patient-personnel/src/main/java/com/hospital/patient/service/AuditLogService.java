package com.hospital.patient.service;

import com.hospital.patient.entity.AuditLog;
import com.hospital.patient.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(String userId, String role, String action, String ressource,
                    Long ressourceId, String details, String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .role(role)
                    .action(action)
                    .ressource(ressource)
                    .ressourceId(ressourceId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .niveau(AuditLog.NiveauLog.INFO)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du log d'audit: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void logCritique(String userId, String role, String action, String details, String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .role(role)
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .niveau(AuditLog.NiveauLog.CRITIQUE)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Erreur audit critique: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
