package com.website.gis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.Entity.User;
import com.website.gis.config.SecurityConfig;
import com.website.gis.dto.LoginRequest;
import com.website.gis.repository.UserRepository;
import com.website.gis.security.CustomUserDetailsService;
import com.website.gis.security.JwtAuthenticationFilter;
import com.website.gis.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void whenLoginWithValidCredentials_thenReturnTokenAndInfo() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        Authentication auth = Mockito.mock(Authentication.class);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "admin", "password", Collections.emptyList()
        );
        Mockito.when(auth.getPrincipal()).thenReturn(userDetails);
        Mockito.when(authenticationManager.authenticate(Mockito.any())).thenReturn(auth);
        Mockito.when(jwtTokenProvider.generateToken(auth)).thenReturn("mock-jwt-token");

        User user = User.builder()
                .username("admin")
                .fullName("Quản trị viên Gia Lai")
                .role("ADMIN")
                .build();
        Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.fullName").value("Quản trị viên Gia Lai"));
    }
}
