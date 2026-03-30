package com.hospital.auth.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre de rate limiting sur les endpoints sensibles d'authentification.
 * Limite à 10 requêtes par minute par IP sur /login, /forgot-password et /register.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final String[] RATE_LIMITED_PATHS = {
        "/api/v1/auth/login",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/register"
    };

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket getBucketForIp(String ip) {
        return buckets.computeIfAbsent(ip, key -> {
            Bandwidth limit = Bandwidth.classic(
                MAX_REQUESTS_PER_MINUTE,
                Refill.greedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
            );
            return Bucket.builder().addLimit(limit).build();
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isRateLimited = false;
        for (String limitedPath : RATE_LIMITED_PATHS) {
            if (path.equals(limitedPath)) {
                isRateLimited = true;
                break;
            }
        }

        if (!isRateLimited) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Bucket bucket = getBucketForIp(clientIp);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit dépassé pour l'IP: {} sur {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Trop de tentatives. Veuillez réessayer dans 1 minute.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
