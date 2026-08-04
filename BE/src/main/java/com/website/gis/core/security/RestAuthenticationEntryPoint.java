package com.website.gis.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi 401 Unauthorized (AuthenticationException) ném ra bởi Spring
 * Security khi request thiếu JWT hoặc JWT không hợp lệ/hết hạn.
 *
 * Cũng như {@link RestAccessDeniedHandler}, lỗi này phát sinh ngay trong
 * filter chain — trước khi request tới DispatcherServlet — nên
 * {@code @RestControllerAdvice} (GlobalExceptionHandler) không bắt được.
 * Trước đây entry point chỉ gọi {@code response.sendError(401, "Unauthorized")},
 * khiến Spring Boot fallback sang whitelabel/BasicErrorController mặc định
 * thay vì payload ErrorResponse chuẩn đã cam kết tại CODING_CONVENTIONS.md
 * (Mục 2) và API_CONTRACT.md (Mục 2).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public RestAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                "Yêu cầu đăng nhập để truy cập tài nguyên này");
    }
}
