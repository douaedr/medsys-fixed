package com.medsys.appointment.service;

import com.medsys.appointment.entity.TimeSlot;
import com.medsys.appointment.entity.WaitingListEntry;
import com.medsys.appointment.repository.TimeSlotRepository;
import com.medsys.appointment.repository.WaitingListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingListService {

    private final WaitingListRepository waitingRepo;
    private final TimeSlotRepository slotRepo;
    private final AppointmentService appointmentService;

    @Transactional
    public WaitingListEntry addToWaitingList(Map<String, Object> req) {
        WaitingListEntry entry = WaitingListEntry.builder()
                .patientId(longVal(req, "patientId"))
                .medecinId(longVal(req, "medecinId"))
                .specialiteId(longVal(req, "specialiteId"))
                .motif(str(req, "motif"))
                .priority(str(req, "priority") != null ? str(req, "priority") : "NORMAL")
                .build();
        return waitingRepo.save(entry);
    }

    public List<WaitingListEntry> getByPatient(Long patientId) {
        return waitingRepo.findByPatientId(patientId);
    }

    public List<WaitingListEntry> getPendingList() {
        return waitingRepo.findByProcessedFalseOrderByCreatedAtAsc();
    }

    @Transactional
    @Scheduled(fixedDelay = 300_000)
    public void processWaitingList() {
        List<WaitingListEntry> pending = waitingRepo.findByProcessedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) return;

        for (WaitingListEntry entry : pending) {
            List<TimeSlot> available = slotRepo.findByMedecinIdAndDebutAfterAndDisponibleTrue(
                    entry.getMedecinId(), LocalDateTime.now());

            if (!available.isEmpty()) {
                TimeSlot slot = available.get(0);
                try {
                    Map<String, Object> req = new HashMap<>();
                    req.put("patientId", entry.getPatientId());
                    req.put("medecinId", entry.getMedecinId());
                    req.put("creneauId", slot.getId());
                    req.put("motif", entry.getMotif());
                    req.put("priority", entry.getPriority());

                    var appointment = appointmentService.createAppointment(req);
                    entry.setProcessed(true);
                    entry.setAssignedAppointmentId(appointment.getId());
                    waitingRepo.save(entry);
                    log.info("[WAITLIST] Matched entry={} to appointment={}", entry.getId(), appointment.getId());
                } catch (Exception e) {
                    log.warn("[WAITLIST] Échec match entry={}: {}", entry.getId(), e.getMessage());
                }
            }
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : null;
    }

    private Long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
