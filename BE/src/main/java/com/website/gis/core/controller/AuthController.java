package com.website.gis.core.controller;

import com.website.gis.core.dto.LoginRequest;
import com.website.gis.core.dto.LoginResponse;
import com.website.gis.core.entity.User;
import com.website.gis.core.mapper.UserMapper;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.JwtTokenProvider;
import com.website.gis.core.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final UserRepository userRepository;
        private final UserMapper userMapper;
        private final LoginAttemptService loginAttemptService;

        /**
         * Bảo mật JWT: token KHÔNG còn được lưu ở localStorage/JS phía FE
         * (dễ bị đánh cắp toàn bộ nếu có bất kỳ lỗ XSS nào, kể cả từ thư viện
         * bên thứ ba). Thay vào đó, token được set qua cookie HttpOnly +
         * Secure + SameSite do BE quản lý, JS không thể đọc được.
         *
         * cookie-secure nên luôn = true ở production (yêu cầu HTTPS).
         * Ở môi trường dev qua HTTP thuần, có thể tạm set JWT_COOKIE_SECURE=false.
         */
        @Value("${app.jwt.cookie-name:gis_token}")
        private String cookieName;

        @Value("${app.jwt.cookie-secure:true}")
        private boolean cookieSecure;

        @Value("${app.jwt.cookie-same-site:Strict}")
        private String cookieSameSite;

        public AuthController(AuthenticationManager authenticationManager,
                        JwtTokenProvider tokenProvider,
                        UserRepository userRepository,
                        UserMapper userMapper,
                        LoginAttemptService loginAttemptService) {
                this.authenticationManager = authenticationManager;
                this.tokenProvider = tokenProvider;
                this.userRepository = userRepository;
                this.userMapper = userMapper;
                this.loginAttemptService = loginAttemptService;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                        HttpServletRequest httpRequest) {
                // Chống brute-force: không có Bucket4j/@Retryable nào trước đây, nên
                // một username (vd. "admin") có thể bị thử mật khẩu không giới hạn số
                // lần. Giới hạn cả theo username lẫn theo IP nguồn (xem LoginAttemptService).
                String usernameKey = "user:" + loginRequest.getUsername().trim().toLowerCase();
                String ipKey = "ip:" + httpRequest.getRemoteAddr();

                loginAttemptService.checkAllowed(usernameKey);
                loginAttemptService.checkAllowed(ipKey);

                try {
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        loginRequest.getUsername(),
                                                        loginRequest.getPassword()));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        String jwt = tokenProvider.generateToken(authentication);

                        User user = userRepository.findByUsername(loginRequest.getUsername())
                                        .orElseThrow(() -> new RuntimeException("User not found after authentication"));

                        ResponseCookie jwtCookie = buildCookie(jwt, tokenProvider.getExpirationMs() / 1000);

                        loginAttemptService.recordSuccess(usernameKey);
                        loginAttemptService.recordSuccess(ipKey);

                        return ResponseEntity.ok()
                                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                        .body(userMapper.toLoginResponse(user));
                } catch (AuthenticationException ex) {
                        loginAttemptService.recordFailure(usernameKey);
                        loginAttemptService.recordFailure(ipKey);
                        throw ex;
                }
        }

        /**
         * Trả thông tin user hiện tại dựa trên JWT trong cookie HttpOnly.
         * Vì FE không đọc được cookie này bằng JS, endpoint này thay thế việc
         * FE tự parse token/localStorage để khôi phục phiên đăng nhập sau khi
         * reload trang (gọi khi app khởi động).
         */
        @GetMapping("/me")
        public ResponseEntity<LoginResponse> getCurrentUser(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                User user = userRepository.findByUsername(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                LoginResponse response = userMapper.toLoginResponse(user);

                return ResponseEntity.ok(response);
        }

        /**
         * Đăng xuất: xóa cookie chứa JWT phía trình duyệt bằng cách set lại
         * cookie cùng tên/path/thuộc tính nhưng Max-Age=0. JWT ở đây không có
         * session/token store phía server nên không có gì để invalidate thêm.
         * Route này tồn tại để khớp API_CONTRACT.md (mục 4.1).
         */
        @PostMapping("/logout")
        public ResponseEntity<Void> logoutUser() {
                ResponseCookie expiredCookie = buildCookie("", 0);
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                                .build();
        }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        String name = cookieName != null ? cookieName : "gis_token";
        String sameSite = cookieSameSite != null ? cookieSameSite : "Strict";
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

}
