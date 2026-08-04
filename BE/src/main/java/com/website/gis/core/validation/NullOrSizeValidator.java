package com.website.gis.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullOrSizeValidator implements ConstraintValidator<NullOrSize, String> {

    private int min;
    private int max;

    @Override
    public void initialize(NullOrSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null hoặc blank -> "không cung cấp giá trị mới", coi là hợp lệ (giữ
        // nguyên giá trị cũ). Chỉ áp min/max khi người dùng THỰC SỰ nhập gì đó.
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        int length = value.length();
        return length >= min && length <= max;
    }
}
