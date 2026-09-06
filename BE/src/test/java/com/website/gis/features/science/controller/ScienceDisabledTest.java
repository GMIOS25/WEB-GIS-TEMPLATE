package com.website.gis.features.science.controller;

import com.website.gis.config.SecurityConfig;
import com.website.gis.config.TestMapperConfig;
import com.website.gis.core.controller.WardController;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.*;
import com.website.gis.core.service.WardService;
import com.website.gis.GisApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WardController.class)
@ContextConfiguration(classes = GisApplication.class)
@TestPropertySource(properties = "features.science.enabled=false")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, TestMapperConfig.class,
        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class ScienceDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WardService wardService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenScienceDisabled_thenEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/science"))
                .andExpect(status().isNotFound());
    }
}
