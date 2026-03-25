package com.hospital.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion par adresse IP.
 * Max 5 tentatives par minute — sans dépendance externe.
 */
@Slf4j
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    /**
     * @return true si la requête est autorisée, false si le rate limit est atteint
     */
    public boolean isAllowed(String ip) {
        long now = Instant.now().toEpochMilli();
        attempts.merge(ip, new ArrayDeque<>(), (existing, empty) -> existing);
        Deque<Long> timestamps = attempts.get(ip);

        synchronized (timestamps) {
            // Supprimer les tentatives hors de la fenêtre
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                log.warn("[RATE-LIMIT] IP bloquée temporairement: {}", ip);
                return false;
            }
            timestamps.addLast(now);
        }
        return true;
    }
}
