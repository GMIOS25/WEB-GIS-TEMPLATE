package com.website.gis.core.controller;

import com.website.gis.core.dto.LoginRequest;
import com.website.gis.core.dto.LoginResponse;
import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.JwtTokenProvider;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final UserRepository userRepository;

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
                        UserRepository userRepository) {
                this.authenticationManager = authenticationManager;
                this.tokenProvider = tokenProvider;
                this.userRepository = userRepository;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getUsername(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = tokenProvider.generateToken(authentication);

                User user = userRepository.findByUsername(loginRequest.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

                ResponseCookie jwtCookie = buildCookie(jwt, tokenProvider.getExpirationMs() / 1000);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                .body(LoginResponse.builder()
                                                .username(user.getUsername())
                                                .fullName(user.getFullName())
                                                .role(user.getRole())
                                                .build());
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

                String role = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .findFirst()
                                .orElse(user.getRole());

                return ResponseEntity.ok(LoginResponse.builder()
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .role(role)
                                .build());
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
                return ResponseCookie.from(cookieName, value)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .sameSite(cookieSameSite)
                                .path("/")
                                .maxAge(maxAgeSeconds)
                                .build();
        }

}
