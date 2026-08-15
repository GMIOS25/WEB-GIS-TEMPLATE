package com.website.gis.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.core.dto.UserCreateRequest;
import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AdminControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.4-alpine").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret",
                () -> "test-secret-key-for-integration-tests-only-must-be-at-least-32-bytes-long");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminCookie;
    private String viewerCookie;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Create admin
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Admin User")
                .role("ADMIN")
                .build();
        userRepository.save(admin);

        // Create viewer
        User viewer = User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("viewer123"))
                .fullName("Viewer User")
                .role("VIEWER")
                .build();
        userRepository.save(viewer);

        // Login as admin
        adminCookie = loginAndGetCookie("admin", "admin123");
        viewerCookie = loginAndGetCookie("viewer", "viewer123");
    }

    private String loginAndGetCookie(String username, String password) throws Exception {
        var request = new com.website.gis.core.dto.LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getCookie("gis_token")
                .getValue();
    }

    @Test
    void adminShouldGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .cookie(new jakarta.servlet.http.Cookie("gis_token", adminCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    void viewerShouldNotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .cookie(new jakarta.servlet.http.Cookie("gis_token", viewerCookie)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setFullName("New User");

        mockMvc.perform(post("/api/admin/users")
                .cookie(new jakarta.servlet.http.Cookie("gis_token", adminCookie))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin"); // Already exists
        request.setPassword("password123");
        request.setFullName("Duplicate User");

        mockMvc.perform(post("/api/admin/users")
                .cookie(new jakarta.servlet.http.Cookie("gis_token", adminCookie))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminShouldDeleteUser() throws Exception {
        User viewer = userRepository.findByUsername("viewer").orElseThrow();

        mockMvc.perform(delete("/api/admin/users/" + viewer.getId())
                .cookie(new jakarta.servlet.http.Cookie("gis_token", adminCookie)))
                .andExpect(status().isOk());

        // Verify user is deleted
        mockMvc.perform(get("/api/admin/users")
                .cookie(new jakarta.servlet.http.Cookie("gis_token", adminCookie)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldNotDeleteOwnAccount() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        mockMvc.perform(delete("/api/admin/users/" + admin.getId())
                .cookie(new jakarta.servlet.http.Cookie("gis_token", adminCookie)))
                .andExpect(status().isBadRequest());
    }
}