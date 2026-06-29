package com.campuscafe.backend.security.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_SECONDS = 15 * 60; // 15 minutes

    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockTimeCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
        lockTimeCache.remove(key);
    }

    public void loginFailed(String key) {
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        attemptsCache.put(key, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            lockTimeCache.put(key, Instant.now().plusSeconds(LOCK_TIME_DURATION_SECONDS));
        }
    }

    public boolean isBlocked(String key) {
        if (lockTimeCache.containsKey(key)) {
            Instant lockExpiry = lockTimeCache.get(key);
            if (Instant.now().isBefore(lockExpiry)) {
                return true;
            } else {
                lockTimeCache.remove(key);
                attemptsCache.remove(key);
            }
        }
        return false;
    }

    public long getRemainingLockMinutes(String key) {
        if (lockTimeCache.containsKey(key)) {
            Instant lockExpiry = lockTimeCache.get(key);
            long secondsLeft = lockExpiry.getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(1, (secondsLeft + 59) / 60);
        }
        return 0;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 1800000)
    public void cleanupExpiredLocks() {
        Instant now = Instant.now();
        lockTimeCache.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        attemptsCache.keySet().removeIf(key -> !lockTimeCache.containsKey(key));
    }
}
