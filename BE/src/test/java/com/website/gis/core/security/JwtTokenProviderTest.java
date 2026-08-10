package com.website.gis.core.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test thuần (không cần Spring context) cho JwtTokenProvider. Field
 * jwtSecret/jwtExpirationInMs được set trực tiếp qua ReflectionTestUtils rồi
 * gọi init() thủ công, vì @PostConstruct không tự chạy khi bean không được
 * Spring container quản lý.
 */
class JwtTokenProviderTest {

    private static final String VALID_SECRET = "a-test-secret-key-that-is-at-least-32-bytes-long!!";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", VALID_SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", 86_400_000L);
        provider.init();
    }

    private Authentication authenticationFor(String username) {
        UserDetails userDetails = new User(username, "irrelevant-password", Collections.emptyList());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void whenSecretShorterThan32Bytes_thenInitFailsFast() {
        JwtTokenProvider shortSecretProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortSecretProvider, "jwtSecret", "too-short");
        ReflectionTestUtils.setField(shortSecretProvider, "jwtExpirationInMs", 86_400_000L);

        // TRƯỚC ĐÂY: init() âm thầm pad secret ngắn bằng byte 0x00 thay vì fail-fast.
        // Giờ phải ném IllegalStateException ngay khi khởi động thay vì âm thầm dùng
        // một khoá ký yếu hơn tưởng tượng.
        assertThrows(IllegalStateException.class, shortSecretProvider::init);
    }

    @Test
    void whenValidToken_thenGenerateAndValidateRoundTrip() {
        String token = provider.generateToken(authenticationFor("admin"));

        assertTrue(provider.validateToken(token));
        assertEquals("admin", provider.getUsernameFromJWT(token));
    }

    @Test
    void whenTokenExpired_thenValidateTokenReturnsFalse() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "jwtSecret", VALID_SECRET);
        // Hạn dùng âm -> token vừa tạo ra đã hết hạn ngay lập tức, tránh phải
        // Thread.sleep() (gây flaky test phụ thuộc thời gian thực thi).
        ReflectionTestUtils.setField(expiredProvider, "jwtExpirationInMs", -10_000L);
        expiredProvider.init();

        String token = expiredProvider.generateToken(authenticationFor("admin"));

        assertFalse(expiredProvider.validateToken(token));
    }

    @Test
    void whenTokenMalformed_thenValidateTokenReturnsFalse() {
        assertFalse(provider.validateToken("this-is-not-a-jwt-token"));
    }

    @Test
    void whenTokenSignedWithDifferentKey_thenValidateTokenReturnsFalse() {
        // Regression test cho lỗi vừa sửa ở validateToken(): TRƯỚC KHI SỬA (catch
        // từng subclass cụ thể - MalformedJwtException/ExpiredJwtException/
        // UnsupportedJwtException - thiếu io.jsonwebtoken.security.SignatureException),
        // test này FAIL vì validateToken() ném SignatureException thoát ra ngoài thay
        // vì trả về false. Ký token bằng MỘT KHOÁ KHÁC rồi validate bằng provider
        // dùng khoá gốc (VALID_SECRET) để mô phỏng token bị giả mạo chữ ký, hoặc
        // JWT_SECRET vừa bị xoay trong khi cookie cũ của người dùng vẫn còn hiệu lực.
        SecretKey otherKey = Keys
                .hmacShaKeyFor("a-completely-different-secret-key-32bytes+".getBytes(StandardCharsets.UTF_8));
        String tokenSignedByOtherKey = Jwts.builder()
                .subject("admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(otherKey)
                .compact();

        assertFalse(provider.validateToken(tokenSignedByOtherKey));
    }
}
