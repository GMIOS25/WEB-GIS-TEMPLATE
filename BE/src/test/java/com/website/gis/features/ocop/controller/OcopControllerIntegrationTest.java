package com.website.gis.features.ocop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.core.dto.LoginRequest;
import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.features.ocop.dto.OcopProductCreateRequest;
import com.website.gis.features.ocop.repository.OcopProductRepository;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class OcopControllerIntegrationTest {

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
        registry.add("features.ocop.enabled", () -> "true");
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
    private OcopProductRepository ocopProductRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminCookie;
    private Cookie viewerCookie;

    @BeforeEach
    void setUp() throws Exception {
        ocopProductRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin user
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Quản trị viên")
                .role("ADMIN")
                .build();
        userRepository.save(admin);

        // Create viewer user
        User viewer = User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("viewer123"))
                .fullName("Người xem")
                .role("VIEWER")
                .build();
        userRepository.save(viewer);

        // Login as admin
        LoginRequest adminLogin = new LoginRequest("admin", "admin123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminCookie = adminResult.getResponse().getCookie("gis_token");

        // Login as viewer
        LoginRequest viewerLogin = new LoginRequest("viewer", "viewer123");
        MvcResult viewerResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viewerLogin)))
                .andExpect(status().isOk())
                .andReturn();
        viewerCookie = viewerResult.getResponse().getCookie("gis_token");
    }

    @Test
    void testOcopCrudLifecycle_withDatabase() throws Exception {
        // Find existing ward code from imported data
        String existingWardCode = wardRepository.findAll().stream()
                .findFirst()
                .map(w -> w.getCode())
                .orElse("21112");

        // 1. Viewer cannot create (403)
        OcopProductCreateRequest createRequest = OcopProductCreateRequest.builder()
                .name("Tiêu Đắk Đoa")
                .productType("Gia vị")
                .description("Hồ tiêu hữu cơ Gia Lai")
                .wardCode(existingWardCode)
                .latitude(new BigDecimal("13.9850"))
                .longitude(new BigDecimal("108.0150"))
                .imageUrl("https://example.com/pepper.jpg")
                .build();

        mockMvc.perform(post("/api/ocop")
                        .cookie(viewerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        // 2. Admin can create (201)
        MvcResult createResult = mockMvc.perform(post("/api/ocop")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tiêu Đắk Đoa"))
                .andExpect(jsonPath("$.latitude").value(13.9850))
                .andExpect(jsonPath("$.longitude").value(108.0150))
                .andReturn();

        Integer createdId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // 3. Viewer can read list and detail (200)
        mockMvc.perform(get("/api/ocop").cookie(viewerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/ocop/" + createdId).cookie(viewerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.name").value("Tiêu Đắk Đoa"));

        // 4. Admin can delete (200)
        mockMvc.perform(delete("/api/ocop/" + createdId).cookie(adminCookie))
                .andExpect(status().isOk());

        // 5. Verify deleted (404)
        mockMvc.perform(get("/api/ocop/" + createdId).cookie(viewerCookie))
                .andExpect(status().isNotFound());
    }
}
