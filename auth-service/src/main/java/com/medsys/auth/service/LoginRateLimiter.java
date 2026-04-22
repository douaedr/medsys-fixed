package com.medsys.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 20;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> windowStart = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        windowStart.putIfAbsent(ip, now);

        if (now - windowStart.get(ip) > WINDOW_MS) {
            attempts.put(ip, new AtomicInteger(0));
            windowStart.put(ip, now);
        }

        attempts.putIfAbsent(ip, new AtomicInteger(0));
        return attempts.get(ip).incrementAndGet() <= MAX_ATTEMPTS;
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        windowStart.entrySet().removeIf(e -> now - e.getValue() > WINDOW_MS * 5);
        attempts.keySet().removeIf(k -> !windowStart.containsKey(k));
    }
}
