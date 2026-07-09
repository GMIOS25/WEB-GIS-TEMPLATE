package com.website.gis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.config.SecurityConfig;
import com.website.gis.dto.UserCreateRequest;
import com.website.gis.dto.UserUpdateRequest;
import com.website.gis.entity.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenViewerAccessAdmin_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminGetAllUsers_thenSuccess() throws Exception {
        User user = User.builder().id(1L).username("viewer").fullName("Viewer Account").role("VIEWER").build();
        Mockito.when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("viewer"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminCreateUser_thenSuccess() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("viewer2");
        request.setPassword("123456");
        request.setFullName("Viewer 2");

        Mockito.when(userRepository.findByUsername("viewer2")).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode("123456")).thenReturn("hashed-pw");

        User saved = User.builder().id(2L).username("viewer2").fullName("Viewer 2").role("VIEWER").build();
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(saved);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.username").value("viewer2"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminUpdateUser_thenSuccess() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFullName("Updated Name");
        request.setPassword("newpassword");

        User existing = User.builder().id(2L).username("viewer2").fullName("Viewer 2").role("VIEWER").build();
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        Mockito.when(passwordEncoder.encode("newpassword")).thenReturn("hashed-new-pw");

        User updated = User.builder().id(2L).username("viewer2").fullName("Updated Name").role("VIEWER").build();
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminDeleteOtherUser_thenSuccess() throws Exception {
        User existing = User.builder().id(2L).username("viewer2").role("VIEWER").build();
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/admin/users/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminDeleteSelf_thenBadRequest() throws Exception {
        User existing = User.builder().id(1L).username("admin").role("ADMIN").build();
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isBadRequest());
    }
}
