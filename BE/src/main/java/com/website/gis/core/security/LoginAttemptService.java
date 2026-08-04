package com.website.gis.core.security;

import com.website.gis.core.exception.TooManyRequestsException;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate-limit / khoá tạm (lockout) số lần đăng nhập sai, theo username VÀ
 * theo IP nguồn, để chống brute-force mật khẩu qua {@code POST /api/auth/login}.
 *
 * TRƯỚC ĐÂY: endpoint login không có bất kỳ giới hạn nào (không Bucket4j,
 * không {@code @Retryable}/lockout logic), nên một tài khoản như "admin" có
 * thể bị thử mật khẩu không giới hạn số lần - đặc biệt nguy hiểm khi kết hợp
 * với rủi ro mật khẩu mặc định yếu ở {@link com.website.gis.config.DatabaseSeeder}.
 *
 * Cài đặt bằng bộ nhớ trong tiến trình ({@link ConcurrentHashMap}) - không
 * cần thêm dependency mới, đủ dùng cho một instance backend duy nhất. Nếu
 * triển khai nhiều instance (scale-out) phía sau load balancer, nên thay
 * bằng một store dùng chung (Redis, Bucket4j + Redis, v.v.) để giới hạn
 * được áp dụng nhất quán giữa các instance.
 */
@Component
public class LoginAttemptService {

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
     * Ném {@link TooManyRequestsException} (-&gt; HTTP 429) nếu key này
     * (username hoặc IP) đang trong thời gian bị khoá tạm. Gọi TRƯỚC khi xác
     * thực mật khẩu để chặn sớm, tránh tốn công verify BCrypt cho các request
     * chắc chắn sẽ bị từ chối.
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

    /** Ghi nhận 1 lần đăng nhập thất bại; khoá tạm key này nếu vượt ngưỡng MAX_ATTEMPTS. */
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
}
