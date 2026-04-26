package com.helpmi.service;

import com.helpmi.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding-window in-memory rate limiter. Single-node only.
 * Buckets are never explicitly evicted; memory footprint is negligible
 * (one small deque per user, at most MAX_REQUESTS entries each).
 */
@Service
public class RateLimiterService {

    private static final int MAX_TOKEN_CREATIONS_PER_HOUR = 10;
    private static final long WINDOW_MILLIS = 60 * 60 * 1000L;

    private final ConcurrentHashMap<UUID, Deque<Instant>> tokenCreationWindows = new ConcurrentHashMap<>();

    public void checkTokenCreation(UUID userId) {
        Instant now = Instant.now();
        Instant cutoff = now.minusMillis(WINDOW_MILLIS);

        Deque<Instant> timestamps = tokenCreationWindows.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            timestamps.removeIf(t -> t.isBefore(cutoff));
            if (timestamps.size() >= MAX_TOKEN_CREATIONS_PER_HOUR) {
                throw new TooManyRequestsException(
                        "Limite atteinte : maximum " + MAX_TOKEN_CREATIONS_PER_HOUR +
                        " tokens créés par heure. Réessayez plus tard.");
            }
            timestamps.addLast(now);
        }
    }
}
