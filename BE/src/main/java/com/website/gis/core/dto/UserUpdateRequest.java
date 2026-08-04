package com.website.gis.core.dto;

import com.website.gis.core.validation.NullOrSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    // optional: null hoặc blank = không đổi mật khẩu (xem AdminController#updateUser).
    // Trước đây field này chỉ có @Size(max = 100) mà thiếu min, nên ADMIN có
    // thể đặt mật khẩu 1 ký tự cho user khác qua PUT /api/admin/users/{id}.
    // Dùng @NullOrSize thay vì @Size(min = 6) vì @Size coi "" (size = 0) là
    // không hợp lệ, trong khi "" ở đây lại có nghĩa hợp lệ là "giữ nguyên".
    @NullOrSize(min = 6, max = 100, message = "Password must be at least 6 and at most 100 characters if provided")
    private String password;
}
