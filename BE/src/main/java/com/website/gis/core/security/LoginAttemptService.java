package com.website.gis.core.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.website.gis.core.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Rate-limit / khoá tạm (lockout) số lần đăng nhập sai, theo username VÀ
 * theo IP nguồn, để chống brute-force mật khẩu qua
 * {@code POST /api/auth/login}.
 * Sử dụng Caffeine Cache với TTL và giới hạn kích thước tối đa để ngăn ngừa rò rỉ bộ nhớ / DoS.
 */
@Component
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public static final class Attempt {
        public volatile int count;
        public volatile Instant windowStartedAt;
        public volatile Instant lockedUntil;
    }

    private final Cache<String, Attempt> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * Ném {@link TooManyRequestsException} (-> HTTP 429) nếu key này
     * (username hoặc IP) đang trong thời gian bị khoá tạm.
     */
    public void checkAllowed(String key) {
        Attempt attempt = attemptsCache.getIfPresent(key);
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
     * Ghi nhận 1 lần đăng nhập thất bại; khoá tạm key này nếu vượt ngưỡng MAX_ATTEMPTS.
     */
    public void recordFailure(String key) {
        Instant now = Instant.now();
        attemptsCache.asMap().compute(key, (k, existing) -> {
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
                logger.warn("Key {} has been temporarily locked due to {} failed login attempts", key, attempt.count);
            }
            return attempt;
        });
    }

    /** Đăng nhập thành công -> xoá lịch sử thất bại của key này. */
    public void recordSuccess(String key) {
        attemptsCache.invalidate(key);
    }
}