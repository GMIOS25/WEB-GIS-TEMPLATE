package com.website.gis.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    // Lưu ý bảo mật: KHÔNG trả JWT trong body JSON nữa.
    // Token được set qua HttpOnly cookie (xem AuthController#authenticateUser),
    // để JS phía FE không thể đọc được token dù có lỗ XSS.
    private String username;
    private String fullName;
    private String role;
}
