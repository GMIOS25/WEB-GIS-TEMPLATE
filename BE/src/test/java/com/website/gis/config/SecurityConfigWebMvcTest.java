package com.website.gis.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.website.gis.core.security.CustomUserDetailsService;
import com.website.gis.core.security.JwtAuthenticationFilter;
import com.website.gis.core.security.JwtTokenProvider;
import com.website.gis.core.security.RestAccessDeniedHandler;
import com.website.gis.core.security.RestAuthenticationEntryPoint;
import com.website.gis.core.security.SecurityErrorResponseWriter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfig.class)
@Import({ JwtAuthenticationFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
        SecurityErrorResponseWriter.class })
class SecurityConfigWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void whenAccessProtectedWithoutToken_thenUnauthorized() throws Exception {
        // Protected endpoint should return 401 Unauthorized when unauthenticated
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessPublicWithoutToken_thenNotBlockedBySecurity() throws Exception {
        // Public endpoint under /api/auth/** should not be blocked by security.
        // Trong slice test chỉ load SecurityConfig (không load AuthController), request
        // rơi vào xử lý lỗi mặc định của ứng dụng và trả 500 thay vì 401.
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void whenAccessActuatorHealthWithoutToken_thenNotBlockedBySecurity() throws Exception {
        // Regression test: Phase1-task.md từng ghi nhầm là endpoint này "đã được phát
        // hiện và sửa" nhưng SecurityConfig thực tế chưa hề có thay đổi - /actuator/health
        // vẫn rơi vào anyRequest().authenticated() và trả 401 cho request ẩn danh, khiến
        // Docker HEALTHCHECK/Caddy/công cụ giám sát uptime không thể dùng được endpoint
        // này như thiết kế. Actuator's auto-config không nằm trong @WebMvcTest slice hẹp
        // này (giống /api/auth/login ở test phía trên), nên yêu cầu chính là không bị
        // chặn ở tầng bảo mật (không trả 401).
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isInternalServerError());
    }
}
