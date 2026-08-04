package com.website.gis.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.core.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Xử lý lỗi 403 Forbidden (AccessDeniedException) ném ra bởi Spring Security
 * khi user đã xác thực nhưng không đủ quyền (vd: VIEWER gọi /api/admin/**).
 *
 * Lỗi này phát sinh ngay trong filter chain của Spring Security, tức là
 * TRƯỚC KHI request tới DispatcherServlet, nên {@code @RestControllerAdvice}
 * (GlobalExceptionHandler) không có cơ hội bắt được. Nếu không khai báo
 * handler riêng, Spring Security sẽ fallback sang trang lỗi whitelabel /
 * BasicErrorController mặc định, sai với payload {@link ErrorResponse} đã
 * cam kết tại CODING_CONVENTIONS.md (Mục 2) và API_CONTRACT.md (Mục 2).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Bạn không có quyền truy cập tài nguyên này")
                .path(request.getRequestURI())
                .details(null)
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
