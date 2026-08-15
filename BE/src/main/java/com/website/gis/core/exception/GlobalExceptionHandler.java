package com.website.gis.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler that converts thrown exceptions into the
 * standardized {@link ErrorResponse} JSON payload described in
 * CODING_CONVENTIONS.md (Section 2) and API_CONTRACT.md (Section 2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn tài nguyên yêu cầu", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ", request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // Handles @Valid DTO Validation Failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "Lỗi xác thực dữ liệu", request, details);
    }

    // Bắt các lỗi xác thực (sai username/password, tài khoản bị khoá, v.v.)
    // BadCredentialsException ném từ AuthenticationManager.authenticate() là
    // subclass của AuthenticationException nên rule này cũng bắt luôn nó.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu", request, null);
    }

    // 403 Forbidden - "đủ điều kiện xác thực nhưng không đủ quyền".
    //
    // Lưu ý: AccessDeniedException ném ra TỪ TRONG filter chain của Spring
    // Security (vd. .hasRole("ADMIN") khai báo ở SecurityConfig, khi VIEWER
    // gọi /api/admin/**) xảy ra TRƯỚC khi request tới DispatcherServlet, nên
    // @RestControllerAdvice này KHÔNG bắt được trường hợp đó - nó đã được xử
    // lý riêng bởi RestAccessDeniedHandler (đăng ký qua
    // .exceptionHandling().accessDeniedHandler(...) trong SecurityConfig),
    // dùng chung SecurityErrorResponseWriter để trả cùng một payload chuẩn.
    //
    // Handler dưới đây là lớp phòng vệ bổ sung (defense-in-depth) cho các
    // trường hợp AccessDeniedException phát sinh SAU khi vào DispatcherServlet
    // - ví dụ nếu sau này có thêm @PreAuthorize/@Secured trên method
    // controller, hoặc service layer tự throw exception này. Nếu không có
    // handler riêng ở đây, những trường hợp đó sẽ rơi xuống
    // handleGeneralException() và trả nhầm 500 thay vì 403, dù bản chất là
    // lỗi phân quyền - sai với API_CONTRACT.md (Mục 2) và khiến client
    // không phân biệt được lỗi hệ thống với lỗi phân quyền.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này", request, null);
    }

    // Đăng nhập sai quá nhiều lần trong một khoảng thời gian ngắn (xem
    // LoginAttemptService) - trả 429 riêng thay vì để rơi xuống 401/500
    // chung chung, giúp client và log phân biệt được đây là rate-limit/
    // lockout chứ không phải do sai mật khẩu ở lần thử này.
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Một lỗi không mong muốn đã xảy ra", request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> details) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build();
        return new ResponseEntity<>(response, status);
    }
}