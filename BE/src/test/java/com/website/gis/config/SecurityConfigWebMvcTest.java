package com.website.gis.config;

import com.website.gis.security.CustomUserDetailsService;
import com.website.gis.security.JwtAuthenticationFilter;
import com.website.gis.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfig.class)
@Import(JwtAuthenticationFilter.class)
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
        // Public endpoint under /api/auth/** should not be blocked by security (so it will return 404 instead of 401)
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isNotFound());
    }
}
