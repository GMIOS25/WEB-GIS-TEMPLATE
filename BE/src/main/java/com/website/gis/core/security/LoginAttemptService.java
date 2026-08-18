package com.website.gis.core.security;

import com.website.gis.core.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate-limit / khoá tạm (lockout) số lần đăng nhập sai, theo username VÀ
 * theo IP nguồn, để chống brute-force mật khẩu qua
 * {@code POST /api/auth/login}.
 */
@Component
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final class Attempt {
        volatile int count;
        volatile Instant windowStartedAt;
        volatile Instant lockedUntil;
    }

    private final ConcurrentMap<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();

    /**
     * Ném {@link TooManyRequestsException} (-> HTTP 429) nếu key này
     * (username hoặc IP) đang trong thời gian bị khoá tạm.
     */
    public void checkAllowed(String key) {
        Attempt attempt = attemptsByKey.get(key);
        if (attempt == null) {
            return;
        }
        Instant lockedUntil = attempt.lockedUntil;
        if (lockedUntil != null && Instant.now().isBefore(lockedUntil)) {
            long secondsLeft = Math.max(1, Duration.between(Instant.now(), lockedUntil).toSeconds());
            throw new TooManyRequestsException(
                    "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + secondsLeft + " giây.");
        }
    }

    /**
     * Ghi nhận 1 lần đăng nhập thất bại; khoá tạm key này nếu vượt ngưỡng
     * MAX_ATTEMPTS.
     */
    public void recordFailure(String key) {
        Instant now = Instant.now();
        attemptsByKey.compute(key, (k, existing) -> {
            Attempt attempt = existing;
            if (attempt == null || attempt.windowStartedAt == null
                    || Duration.between(attempt.windowStartedAt, now).compareTo(ATTEMPT_WINDOW) > 0) {
                attempt = new Attempt();
                attempt.windowStartedAt = now;
                attempt.count = 0;
            }
            attempt.count++;
            if (attempt.count >= MAX_ATTEMPTS) {
                attempt.lockedUntil = now.plus(LOCK_DURATION);
            }
            return attempt;
        });
    }

    /** Đăng nhập thành công -> xoá lịch sử thất bại của key này. */
    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredAttempts() {
        Instant now = Instant.now();
        attemptsByKey.entrySet().removeIf(entry -> {
            Attempt attempt = entry.getValue();

            // If still locked, keep it
            if (attempt.lockedUntil != null && now.isBefore(attempt.lockedUntil)) {
                return false;
            }

            // If window has expired, remove it
            if (attempt.windowStartedAt != null) {
                Duration age = Duration.between(attempt.windowStartedAt, now);
                Duration maxAge = ATTEMPT_WINDOW.plus(LOCK_DURATION);
                return age.compareTo(maxAge) > 0;
            }

            return false;
        });
        logger.debug("Expired login attempt cleanup completed");
    }
}