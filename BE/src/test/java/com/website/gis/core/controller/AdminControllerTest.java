package com.website.gis.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.config.SecurityConfig;
import com.website.gis.config.TestMapperConfig;
import com.website.gis.core.dto.UserCreateRequest;
import com.website.gis.core.dto.UserDto;
import com.website.gis.core.dto.UserUpdateRequest;
import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.CustomUserDetailsService;
import com.website.gis.core.security.JwtAuthenticationFilter;
import com.website.gis.core.security.JwtTokenProvider;
import com.website.gis.core.security.RestAccessDeniedHandler;
import com.website.gis.core.security.RestAuthenticationEntryPoint;
import com.website.gis.core.security.SecurityErrorResponseWriter;
import com.website.gis.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, TestMapperConfig.class, RestAccessDeniedHandler.class,
        RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

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
        UserDto userDto = UserDto.builder().id(1L).username("viewer").fullName("Viewer Account").role("VIEWER").build();
        Mockito.when(userService.getAllUsers()).thenReturn(Collections.singletonList(userDto));

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

        UserDto created = UserDto.builder().id(2L).username("viewer2").fullName("Viewer 2").role("VIEWER").build();
        Mockito.when(userService.createUser(any(UserCreateRequest.class))).thenReturn(created);

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

        UserDto updated = UserDto.builder().id(2L).username("viewer2").fullName("Updated Name").role("VIEWER").build();
        Mockito.when(userService.updateUser(eq(2L), any(UserUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminDeleteOtherUser_thenSuccess() throws Exception {
        Mockito.doNothing().when(userService).deleteUser(2L, "admin");

        mockMvc.perform(delete("/api/admin/users/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenAdminDeleteSelf_thenBadRequest() throws Exception {
        Mockito.doThrow(new BadRequestException("You cannot delete your own account"))
                .when(userService).deleteUser(1L, "admin");

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isBadRequest());
    }
}
