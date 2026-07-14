package com.website.gis.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.config.SecurityConfig;
import com.website.gis.core.controller.AuthController;
import com.website.gis.core.dto.LoginRequest;
import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.CustomUserDetailsService;
import com.website.gis.core.security.JwtAuthenticationFilter;
import com.website.gis.core.security.JwtTokenProvider;

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
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
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
                                "admin", "password", Collections.emptyList());
                Mockito.when(auth.getPrincipal()).thenReturn(userDetails);
                Mockito.when(authenticationManager.authenticate(Mockito.any())).thenReturn(auth);
                Mockito.when(jwtTokenProvider.generateToken(auth)).thenReturn("mock-jwt-token");
                Mockito.when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

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
                                // Token không còn nằm trong JSON body -> chỉ nằm trong cookie HttpOnly
                                .andExpect(jsonPath("$.token").doesNotExist())
                                .andExpect(cookie().exists("gis_token"))
                                .andExpect(cookie().httpOnly("gis_token", true))
                                .andExpect(cookie().value("gis_token", "mock-jwt-token"))
                                .andExpect(jsonPath("$.username").value("admin"))
                                .andExpect(jsonPath("$.role").value("ADMIN"))
                                .andExpect(jsonPath("$.fullName").value("Quản trị viên Gia Lai"));
        }

        @Test
        void whenLoginWithWrongPassword_thenReturn401WithProperMessage() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setUsername("admin");
                request.setPassword("wrong-password");

                Mockito.when(authenticationManager.authenticate(Mockito.any()))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message").value("Sai tên đăng nhập hoặc mật khẩu"));
        }
}
