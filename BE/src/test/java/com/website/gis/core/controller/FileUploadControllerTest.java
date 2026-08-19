package com.website.gis.core.controller;

import com.website.gis.config.SecurityConfig;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.*;
import com.website.gis.core.storage.FileStorageService;
import com.website.gis.core.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, RestAccessDeniedHandler.class,
        RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void whenUnauthenticated_thenRejectUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenUploadAsViewer_thenReturn403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenUploadAsAdmin_thenReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        StoredFile storedFile = new StoredFile("test-uuid.png", "test.png", "image/png", 3, "/api/files/test-uuid.png");

        Mockito.when(fileStorageService.store(any(), any())).thenReturn(storedFile);

        mockMvc.perform(multipart("/api/files").file(file).param("folder", "ocop"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storedFileName").value("test-uuid.png"))
                .andExpect(jsonPath("$.publicUrl").value("/api/files/test-uuid.png"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenServeFile_asAuthenticatedUser_thenReturnResource() throws Exception {
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "test.png";
            }
        };

        Mockito.when(fileStorageService.loadAsResource("test.png")).thenReturn(resource);

        mockMvc.perform(get("/api/files/test.png"))
                .andExpect(status().isOk());
    }
}
