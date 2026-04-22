package com.medsys.appointment.service;

import com.medsys.appointment.entity.TimeSlot;
import com.medsys.appointment.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@Transactional
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository slotRepo;

    public TimeSlot create(Map<String, Object> req) {
        TimeSlot slot = TimeSlot.builder()
                .medecinId(longVal(req, "medecinId"))
                .debut(parseDateTime(req, "debut"))
                .fin(parseDateTime(req, "fin"))
                .disponible(true)
                .dureeMinutes(intVal(req, "dureeMinutes", 30))
                .type(str(req, "type", "CONSULTATION"))
                .specialiteId(longVal(req, "specialiteId"))
                .build();
        return slotRepo.save(slot);
    }

    public List<TimeSlot> getAvailableByMedecin(Long medecinId) {
        return slotRepo.findByMedecinIdAndDebutAfterAndDisponibleTrue(medecinId, LocalDateTime.now());
    }

    public List<TimeSlot> getAllAvailable() {
        return slotRepo.findByDisponibleTrueAndDebutAfter(LocalDateTime.now());
    }

    public TimeSlot getById(Long id) {
        return slotRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Créneau non trouvé: " + id));
    }

    public void delete(Long id) {
        slotRepo.deleteById(id);
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key); return v != null ? v.toString() : def;
    }

    private Long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private LocalDateTime parseDateTime(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) throw new IllegalArgumentException(key + " requis");
        return LocalDateTime.parse(v.toString().substring(0, 19));
    }
}
