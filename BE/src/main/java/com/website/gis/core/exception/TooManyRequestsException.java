package com.website.gis.core.exception;

/**
 * Ném ra khi một username hoặc IP nguồn bị tạm khoá do đăng nhập sai quá
 * nhiều lần trong một khoảng thời gian ngắn. Được {@link GlobalExceptionHandler}
 * map sang {@code 429 Too Many Requests} theo đúng payload {@link ErrorResponse}
 * chuẩn.
 *
 * @see com.website.gis.core.security.LoginAttemptService
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
