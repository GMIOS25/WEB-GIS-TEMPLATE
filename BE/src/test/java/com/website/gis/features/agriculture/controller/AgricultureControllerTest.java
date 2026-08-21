package com.website.gis.features.agriculture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.config.SecurityConfig;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.security.*;
import com.website.gis.features.agriculture.dto.AgricultureUnitCreateRequest;
import com.website.gis.features.agriculture.dto.AgricultureUnitUpdateRequest;
import com.website.gis.features.agriculture.entity.AgricultureUnit;
import com.website.gis.features.agriculture.mapper.AgricultureUnitMapperImpl;
import com.website.gis.features.agriculture.repository.AgricultureUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgricultureController.class)
@TestPropertySource(properties = "features.agriculture.enabled=true")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, AgricultureUnitMapperImpl.class,
        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class AgricultureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgricultureUnitRepository agricultureUnitRepository;

    @MockitoBean
    private WardRepository wardRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private AgricultureUnit sampleUnit;
    private Ward sampleWard;

    @BeforeEach
    void setUp() {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = gf.createPoint(new Coordinate(108.0123, 13.9876));

        sampleWard = Ward.builder()
                .code("21112")
                .name("Xã An Phú")
                .build();

        sampleUnit = AgricultureUnit.builder()
                .id(1)
                .name("Trang trại Cà phê Đak Đoa")
                .unitType("Trang trại")
                .description("Cà phê Robusta chất lượng cao")
                .ward(sampleWard)
                .geom(point)
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
        Mockito.when(agricultureUnitRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleUnit)));

        mockMvc.perform(get("/api/agriculture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Trang trại Cà phê Đak Đoa"))
                .andExpect(jsonPath("$.content[0].wardCode").value("21112"))
                .andExpect(jsonPath("$.content[0].latitude").value(13.9876))
                .andExpect(jsonPath("$.content[0].longitude").value(108.0123));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetAgricultureGeoJson_asViewer_thenReturnFeatureCollection() throws Exception {
        Mockito.when(agricultureUnitRepository.findAll()).thenReturn(List.of(sampleUnit));

        mockMvc.perform(get("/api/agriculture/geojson"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.features[0].geometry.type").value("Point"))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(108.0123))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[1]").value(13.9876))
                .andExpect(jsonPath("$.features[0].properties.id").value(1))
                .andExpect(jsonPath("$.features[0].properties.name").value("Trang trại Cà phê Đak Đoa"))
                .andExpect(jsonPath("$.features[0].properties.unitType").value("Trang trại"))
                .andExpect(jsonPath("$.features[0].properties.wardCode").value("21112"))
                .andExpect(jsonPath("$.features[0].properties.imageUrl").value("https://example.com/agri.jpg"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_validParams_thenReturnList() throws Exception {
        Mockito.when(agricultureUnitRepository.findNearby(13.9876, 108.0123, 10000.0))
                .thenReturn(List.of(sampleUnit));

        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "108.0123")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Trang trại Cà phê Đak Đoa"))
                .andExpect(jsonPath("$[0].latitude").value(13.9876))
                .andExpect(jsonPath("$[0].longitude").value(108.0123));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_invalidLat_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "95.0")
                        .param("lng", "108.0123")
                        .param("radiusKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_invalidLng_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "-190.0")
                        .param("radiusKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyAgricultureUnits_invalidRadiusKm_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/agriculture/nearby")
                        .param("lat", "13.9876")
                        .param("lng", "108.0123")
                        .param("radiusKm", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetById_asViewer_thenReturnSingle() throws Exception {
        Mockito.when(agricultureUnitRepository.findById(1)).thenReturn(Optional.of(sampleUnit));

        mockMvc.perform(get("/api/agriculture/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Trang trại Cà phê Đak Đoa"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenCreate_asViewer_thenReturn403() throws Exception {
        AgricultureUnitCreateRequest request = AgricultureUnitCreateRequest.builder()
                .name("Hợp tác xã Nông nghiệp")
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
                .name("Trang trại Cà phê Đak Đoa")
                .unitType("Trang trại")
                .description("Cà phê Robusta chất lượng cao")
                .wardCode("21112")
                .latitude(new BigDecimal("13.9876"))
                .longitude(new BigDecimal("108.0123"))
                .imageUrl("https://example.com/agri.jpg")
                .build();

        Mockito.when(wardRepository.findById("21112")).thenReturn(Optional.of(sampleWard));
        Mockito.when(agricultureUnitRepository.save(any(AgricultureUnit.class))).thenReturn(sampleUnit);

        mockMvc.perform(post("/api/agriculture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trang trại Cà phê Đak Đoa"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenUpdate_asAdmin_thenReturn200() throws Exception {
        AgricultureUnitUpdateRequest request = AgricultureUnitUpdateRequest.builder()
                .name("Tên mới")
                .build();

        Mockito.when(agricultureUnitRepository.findById(1)).thenReturn(Optional.of(sampleUnit));
        Mockito.when(agricultureUnitRepository.save(any(AgricultureUnit.class))).thenReturn(sampleUnit);

        mockMvc.perform(put("/api/agriculture/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenDelete_asAdmin_thenReturn200() throws Exception {
        Mockito.when(agricultureUnitRepository.findById(1)).thenReturn(Optional.of(sampleUnit));

        mockMvc.perform(delete("/api/agriculture/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa đơn vị nông nghiệp thành công"));
    }
}

