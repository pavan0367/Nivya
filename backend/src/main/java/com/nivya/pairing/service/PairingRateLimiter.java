package com.nivya.pairing.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe rate limiter and brute-force protection for pairing code attempts.
 * Limits failed attempts to 5 per 15-minute window per subject.
 */
@Component
public class PairingRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 15 * 60; // 15 minutes

    private static class AttemptRecord {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();

        public boolean incrementAndCheckExceeded() {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS))) {
                synchronized (this) {
                    if (now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS))) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
            int current = count.incrementAndGet();
            return current > MAX_ATTEMPTS;
        }

        public boolean isBlocked() {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS))) {
                return false;
            }
            return count.get() >= MAX_ATTEMPTS;
        }

        public void reset() {
            count.set(0);
            windowStart = Instant.now();
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> trackers = new ConcurrentHashMap<>();

    public boolean isRateLimited(String key) {
        AttemptRecord record = trackers.get(key);
        return record != null && record.isBlocked();
    }

    public void recordFailedAttempt(String key) {
        trackers.computeIfAbsent(key, k -> new AttemptRecord()).incrementAndCheckExceeded();
    }

    public void recordSuccess(String key) {
        AttemptRecord record = trackers.get(key);
        if (record != null) {
            record.reset();
        }
    }

    public void clearAll() {
        trackers.clear();
    }
}
