package com.website.gis.config;

import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seed 2 tài khoản mặc định (admin/viewer) khi bảng users rỗng.
 *
 * TRƯỚC ĐÂY: bean này seed VÔ ĐIỀU KIỆN ở MỌI môi trường (kể cả production)
 * mỗi khi userRepository.count() == 0, với mật khẩu "123456" hardcode thẳng
 * trong code. Kết hợp với việc AuthController không có bất kỳ rate-limit/
 * lockout nào (xem LoginAttemptService), một tài khoản ADMIN với mật khẩu ai
 * cũng biết trước có thể bị brute-force gần như ngay lập tức nếu lỡ deploy
 * production mà quên xoá/đổi 2 tài khoản này.
 *
 * BÂY GIỜ (an toàn theo mặc định - "secure by default"):
 * 1) Seeding CHỈ chạy khi được bật tường minh qua app.seed.enabled=true
 * (biến môi trường SEED_DEFAULT_ACCOUNTS). Không set gì -> mặc định false
 * -> KHÔNG seed, kể cả ở production, kể cả khi bảng users rỗng.
 * 2) Không còn mật khẩu mặc định hardcode trong code. Khi seeding được bật,
 * mật khẩu admin/viewer BẮT BUỘC phải cấp qua biến môi trường
 * (SEED_ADMIN_PASSWORD / SEED_VIEWER_PASSWORD); nếu thiếu hoặc quá ngắn,
 * ứng dụng fail-fast (không âm thầm seed mật khẩu yếu/rỗng).
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.viewer-username:viewer}")
    private String viewerUsername;

    @Value("${app.seed.viewer-password:}")
    private String viewerPassword;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            logger.debug("app.seed.enabled=false -> bỏ qua seed tài khoản mặc định.");
            return;
        }

        requireStrongSeedPassword("app.seed.admin-password (SEED_ADMIN_PASSWORD)", adminPassword);
        requireStrongSeedPassword("app.seed.viewer-password (SEED_VIEWER_PASSWORD)", viewerPassword);

        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName("Quản trị viên Gia Lai")
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);

            User viewer = User.builder()
                    .username(viewerUsername)
                    .password(passwordEncoder.encode(viewerPassword))
                    .fullName("Người xem bản đồ")
                    .role("VIEWER")
                    .build();
            userRepository.save(viewer);

            logger.warn("Database seeded with accounts ('{}' / '{}'). Chỉ dùng cho môi trường dev/test - " +
                    "KHÔNG bật app.seed.enabled ở production.", adminUsername, viewerUsername);
        }
    }

    private void requireStrongSeedPassword(String propertyLabel, String password) {
        if (!StringUtils.hasText(password) || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "app.seed.enabled=true nhưng " + propertyLabel + " chưa được set hoặc ngắn hơn "
                            + MIN_PASSWORD_LENGTH
                            + " ký tự. Từ chối khởi động thay vì tự seed mật khẩu yếu/rỗng cho tài khoản mặc định.");
        }
    }
}
