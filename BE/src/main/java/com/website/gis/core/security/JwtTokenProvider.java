package com.website.gis.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private SecretKey key;

    /**
     * Dùng để tính Max-Age cho cookie HttpOnly chứa JWT (xem AuthController).
     */
    public long getExpirationMs() {
        return jwtExpirationInMs;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // TRƯỚC ĐÂY: tự động pad thêm byte 0x00 cho đủ 32 byte khi secret
            // ngắn hơn yêu cầu của thuật toán HS256 (256 bit). Việc này ÂM
            // THẦM làm yếu key (phần đệm 0x00 không thêm entropy) mà không có
            // bất kỳ log/exception nào cảnh báo - nếu ai đó lỡ set JWT_SECRET
            // ngắn ở production, ứng dụng vẫn khởi động bình thường với một
            // khoá ký JWT yếu hơn nhiều so với tưởng tượng.
            //
            // Fail-fast: từ chối khởi động ngay lập tức thay vì tự vá, buộc
            // phải cấu hình JWT_SECRET đủ mạnh (>= 32 byte / 256 bit) trước
            // khi lên bất kỳ môi trường nào.
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) phải dài tối thiểu 32 byte (256 bit) để dùng với thuật toán ký "
                            + "HS256, nhưng giá trị hiện tại chỉ có " + keyBytes.length + " byte. "
                            + "Hãy đặt JWT_SECRET là một chuỗi ngẫu nhiên đủ dài (khuyến nghị >= 64 ký tự).");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        logger.info("JwtTokenProvider initialized with a {}-byte signing key.", keyBytes.length);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String roles = userPrincipal.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException ex) {
            // TRƯỚC ĐÂY: liệt kê riêng từng subclass cụ thể (MalformedJwtException,
            // ExpiredJwtException, UnsupportedJwtException) nhưng THIẾU
            // io.jsonwebtoken.security.SignatureException - exception này được ném ra
            // khi chữ ký JWT không khớp key hiện tại (token bị giả mạo, hoặc JWT_SECRET
            // vừa bị xoay trong khi cookie cũ của người dùng vẫn còn hiệu lực). Vì
            // SignatureException KHÔNG phải là subclass của 3 loại trên, nó sẽ thoát
            // khỏi phương thức này thay vì trả về false như hợp đồng của hàm.
            //
            // Blast radius trên thực tế được chặn lại bởi lớp catch-all
            // (catch (Exception ex)) ở JwtAuthenticationFilter#doFilterInternal - nên
            // người dùng vẫn chỉ thấy 401 như bình thường - nhưng dựa vào một lớp
            // phòng thủ Ở NƠI KHÁC để che một lỗi logic ngay tại đây là mong manh: nếu
            // sau này validateToken() được gọi từ chỗ khác không có try-catch bao
            // ngoài, lỗi sẽ lộ ra thành 500 thay vì 401.
            //
            // Bắt theo JwtException (lớp cha chung của MỌI exception mà jjwt tự ném ra
            // khi parse/verify: SignatureException, MalformedJwtException,
            // ExpiredJwtException, UnsupportedJwtException, WeakKeyException...) loại
            // bỏ hẳn rủi ro "quên liệt kê" một subclass mới về sau.
            logger.warn("Invalid JWT token ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty.");
        }
        return false;
    }
}
