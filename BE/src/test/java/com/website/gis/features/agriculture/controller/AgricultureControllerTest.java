package com.website.gis.features.agriculture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.config.SecurityConfig;
import com.website.gis.config.TestMapperConfig;
import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.*;
import com.website.gis.features.agriculture.dto.AgricultureUnitCreateRequest;
import com.website.gis.features.agriculture.dto.AgricultureUnitDto;
import com.website.gis.features.agriculture.dto.AgricultureUnitUpdateRequest;
import com.website.gis.features.agriculture.service.AgricultureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgricultureController.class)
@TestPropertySource(properties = "features.agriculture.enabled=true")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, TestMapperConfig.class,
        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class AgricultureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgricultureService agricultureService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private AgricultureUnitDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = AgricultureUnitDto.builder()
                .id(1)
                .name("Hợp tác xã Nông nghiệp Chư Păh")
                .unitType("Hợp tác xã")
                .description("Sản xuất cà phê hữu cơ")
                .wardCode("21112")
                .latitude(new BigDecimal("13.9876"))
                .longitude(new BigDecimal("108.0123"))
                .imageUrl("https://example.com/agri.jpg")
                .build();
    }

    @Test
    void whenUnauthenticated_thenReject() throws Exception {
        mockMvc.perform(get("/api/agriculture"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetAll_asViewer_thenReturnPage() throws Exception {
        Mockito.when(agricultureService.getAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleDto)));

        mockMvc.perform(get("/api/agriculture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Hợp tác xã Nông nghiệp Chư Păh"))
                .andExpect(jsonPath("$.content[0].unitType").value("Hợp tác xã"))
                .andExpect(jsonPath("$.content[0].wardCode").value("21112"))
                .andExpect(jsonPath("$.content[0].latitude").value(13.9876))
                .andExpect(jsonPath("$.content[0].longitude").value(108.0123));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetAgricultureGeoJson_asViewer_thenReturnFeatureCollection() throws Exception {
        String mockGeoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "Point",
                        "coordinates": [108.0123, 13.9876]
                      },
                      "properties": {
                        "id": 1,
                        "name": "Hợp tác xã Nông nghiệp Chư Păh",
                        "unitType": "Hợp tác xã",
                        "wardCode": "21112",
                        "imageUrl": "https://example.com/agri.jpg"
                      }
                    }
                  ]
                }
                """;

        Mockito.when(agricultureService.getGeoJson()).thenReturn(mockGeoJson);

        mockMvc.perform(get("/api/agriculture/geojson"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.features[0].geometry.type").value("Point"))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(108.0123))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[1]").value(13.9876))
                .andExpect(jsonPath("$.features[0].properties.id").value(1))
                .andExpect(jsonPath("$.features[0].properties.name").value("Hợp tác xã Nông nghiệp Chư Păh"))
                .andExpect(jsonPath("$.features[0].properties.unitType").value("Hợp tác xã"))
                .andExpect(jsonPath("$.features[0].properties.wardCode").value("21112"))
                .andExpect(jsonPath("$.features[0].properties.imageUrl").value("https://example.com/agri.jpg"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_validParams_thenReturnList() throws Exception {
        Mockito.when(agricultureService.getNearby(13.9876, 108.0123, 10.0))
                .thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "108.0123")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Hợp tác xã Nông nghiệp Chư Păh"))
                .andExpect(jsonPath("$[0].latitude").value(13.9876))
                .andExpect(jsonPath("$[0].longitude").value(108.0123));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_emptyResults_thenReturnEmptyList() throws Exception {
        Mockito.when(agricultureService.getNearby(13.9876, 108.0123, 10.0))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "108.0123")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_invalidLat_thenReturn400() throws Exception {
        Mockito.when(agricultureService.getNearby(95.0, 108.0123, 10.0))
                .thenThrow(new BadRequestException("Vĩ độ (lat) không hợp lệ (phải từ -90 đến 90)"));

        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "95.0")
                        .param("lng", "108.0123")
                        .param("radiusKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_invalidLng_thenReturn400() throws Exception {
        Mockito.when(agricultureService.getNearby(13.9876, -190.0, 10.0))
                .thenThrow(new BadRequestException("Kinh độ (lng) không hợp lệ (phải từ -180 đến 180)"));

        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "-190.0")
                        .param("radiusKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_invalidRadiusKm_thenReturn400() throws Exception {
        Mockito.when(agricultureService.getNearby(13.9876, 108.0123, 0.0))
                .thenThrow(new BadRequestException("Bán kính (radiusKm) phải lớn hơn 0"));

        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "108.0123")
                        .param("radiusKm", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetById_asViewer_thenReturnSingle() throws Exception {
        Mockito.when(agricultureService.getById(1)).thenReturn(sampleDto);

        mockMvc.perform(get("/api/agriculture/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hợp tác xã Nông nghiệp Chư Păh"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenCreate_asViewer_thenReturn403() throws Exception {
        AgricultureUnitCreateRequest request = AgricultureUnitCreateRequest.builder()
                .name("Hợp tác xã")
                .wardCode("21112")
                .latitude(new BigDecimal("13.98"))
                .longitude(new BigDecimal("108.01"))
                .build();

        mockMvc.perform(post("/api/agriculture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenCreate_asAdmin_thenReturn201() throws Exception {
        AgricultureUnitCreateRequest request = AgricultureUnitCreateRequest.builder()
                .name("Hợp tác xã Nông nghiệp Chư Păh")
                .unitType("Hợp tác xã")
                .description("Sản xuất cà phê hữu cơ")
                .wardCode("21112")
                .latitude(new BigDecimal("13.9876"))
                .longitude(new BigDecimal("108.0123"))
                .imageUrl("https://example.com/agri.jpg")
                .build();

        Mockito.when(agricultureService.create(any(AgricultureUnitCreateRequest.class))).thenReturn(sampleDto);

        mockMvc.perform(post("/api/agriculture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hợp tác xã Nông nghiệp Chư Păh"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenUpdate_asAdmin_thenReturn200() throws Exception {
        AgricultureUnitUpdateRequest request = AgricultureUnitUpdateRequest.builder()
                .name("Tên mới")
                .build();

        Mockito.when(agricultureService.update(eq(1), any(AgricultureUnitUpdateRequest.class))).thenReturn(sampleDto);

        mockMvc.perform(put("/api/agriculture/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenDelete_asAdmin_thenReturn200() throws Exception {
        Mockito.doNothing().when(agricultureService).delete(1);

        mockMvc.perform(delete("/api/agriculture/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa cơ sở nông nghiệp thành công"));
    }
}
