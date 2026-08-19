package com.website.gis.features.ocop.controller;

import com.website.gis.config.SecurityConfig;
import com.website.gis.core.controller.WardController;
import com.website.gis.core.mapper.WardMapperImpl;
import com.website.gis.core.repository.GisWardRepository;
import com.website.gis.core.repository.LocalLeaderRepository;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WardController.class)
@TestPropertySource(properties = "features.ocop.enabled=false")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, WardMapperImpl.class,
        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class OcopDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WardRepository wardRepository;

    @MockitoBean
    private GisWardRepository gisWardRepository;

    @MockitoBean
    private LocalLeaderRepository localLeaderRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenOcopDisabled_thenEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/ocop"))
                .andExpect(status().isNotFound());
    }
}
