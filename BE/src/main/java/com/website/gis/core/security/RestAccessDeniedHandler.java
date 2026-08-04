package com.website.gis.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi 403 Forbidden (AccessDeniedException) ném ra bởi Spring Security
 * khi user đã xác thực nhưng không đủ quyền (vd: VIEWER gọi /api/admin/**).
 *
 * Lỗi này phát sinh ngay trong filter chain của Spring Security, tức là
 * TRƯỚC KHI request tới DispatcherServlet, nên {@code @RestControllerAdvice}
 * (GlobalExceptionHandler) không có cơ hội bắt được. Nếu không khai báo
 * handler riêng, Spring Security sẽ fallback sang trang lỗi whitelabel /
 * BasicErrorController mặc định, sai với payload ErrorResponse đã cam kết
 * tại CODING_CONVENTIONS.md (Mục 2) và API_CONTRACT.md (Mục 2).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public RestAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        errorResponseWriter.write(request, response, HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này");
    }
}
