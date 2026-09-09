package com.nivya.auth.service;

import com.nivya.common.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe rate limiter and brute-force protection for authentication endpoints.
 * Limits failed login attempts to 5 per 15-minute sliding window per IP and email.
 */
@Component
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
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
            return current > MAX_FAILED_ATTEMPTS;
        }

        public boolean isBlocked() {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS))) {
                return false;
            }
            return count.get() >= MAX_FAILED_ATTEMPTS;
        }

        public void reset() {
            count.set(0);
            windowStart = Instant.now();
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> ipTrackers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptRecord> emailTrackers = new ConcurrentHashMap<>();

    /**
     * Checks whether the client IP or email is currently rate limited.
     * Throws RateLimitExceededException if blocked.
     */
    public void checkRateLimit(String ip, String email) {
        if (ip != null && !ip.isBlank()) {
            AttemptRecord ipRecord = ipTrackers.get(ip);
            if (ipRecord != null && ipRecord.isBlocked()) {
                log.warn("Rate limit exceeded for IP: {}", ip);
                throw new RateLimitExceededException("Too many failed authentication attempts from this IP. Please wait 15 minutes.");
            }
        }

        if (email != null && !email.isBlank()) {
            AttemptRecord emailRecord = emailTrackers.get(email.toLowerCase().trim());
            if (emailRecord != null && emailRecord.isBlocked()) {
                log.warn("Rate limit exceeded for email: {}", email);
                throw new RateLimitExceededException("Too many failed authentication attempts for this account. Please wait 15 minutes.");
            }
        }
    }

    /**
     * Records a failed authentication attempt for both IP and email.
     */
    public void recordFailedAttempt(String ip, String email) {
        if (ip != null && !ip.isBlank()) {
            ipTrackers.computeIfAbsent(ip, k -> new AttemptRecord()).incrementAndCheckExceeded();
        }
        if (email != null && !email.isBlank()) {
            emailTrackers.computeIfAbsent(email.toLowerCase().trim(), k -> new AttemptRecord()).incrementAndCheckExceeded();
        }
    }

    /**
     * Resets failed attempt counters upon successful authentication.
     */
    public void recordSuccess(String ip, String email) {
        if (ip != null && !ip.isBlank()) {
            AttemptRecord ipRecord = ipTrackers.get(ip);
            if (ipRecord != null) {
                ipRecord.reset();
            }
        }
        if (email != null && !email.isBlank()) {
            AttemptRecord emailRecord = emailTrackers.get(email.toLowerCase().trim());
            if (emailRecord != null) {
                emailRecord.reset();
            }
        }
    }

    /**
     * Clears all trackers (used in integration tests).
     */
    public void clearAll() {
        ipTrackers.clear();
        emailTrackers.clear();
    }
}
