package com.website.gis.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.core.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Helper dùng chung để build và ghi payload {@link ErrorResponse} chuẩn cho
 * các lỗi phát sinh ngay trong filter chain của Spring Security (401 - chưa
 * xác thực, 403 - không đủ quyền), tức là TRƯỚC KHI request tới
 * DispatcherServlet nên {@code @RestControllerAdvice} (GlobalExceptionHandler)
 * không bắt được.
 *
 * Được dùng bởi {@link RestAuthenticationEntryPoint} (401) và
 * {@link RestAccessDeniedHandler} (403) để đảm bảo cả hai loại lỗi cùng trả
 * về đúng một định dạng, theo cam kết tại CODING_CONVENTIONS.md (Mục 2) và
 * API_CONTRACT.md (Mục 2).
 */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .details(null)
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
