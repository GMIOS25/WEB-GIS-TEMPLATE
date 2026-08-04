package com.website.gis.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Giống {@code @Size}, nhưng coi {@code null} HOẶC chuỗi rỗng/blank (sau khi
 * trim) là hợp lệ - dùng cho các field optional kiểu "chỉ cập nhật nếu có
 * giá trị" (vd. {@code UserUpdateRequest.password}).
 *
 * Lý do không dùng {@code @Size(min = ...)} trực tiếp: Bean Validation coi
 * chuỗi rỗng "" có size = 0, nên {@code @Size(min = 6)} sẽ từ chối cả trường
 * hợp "để trống nghĩa là giữ nguyên mật khẩu cũ" - đúng ra chỉ cần chặn
 * trường hợp người dùng CÓ NHẬP nhưng nhập quá ngắn (1-5 ký tự).
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NullOrSizeValidator.class)
@Documented
public @interface NullOrSize {

    String message() default "must be between {min} and {max} characters if provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 0;

    int max() default Integer.MAX_VALUE;
}
