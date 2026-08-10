package com.website.gis.core.controller;

import com.website.gis.config.SecurityConfig;
import com.website.gis.core.controller.WardController;
import com.website.gis.core.entity.GisWard;
import com.website.gis.core.entity.Province;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.mapper.WardMapperImpl;
import com.website.gis.core.repository.GisWardRepository;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.security.CustomUserDetailsService;
import com.website.gis.core.security.JwtAuthenticationFilter;
import com.website.gis.core.security.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WardController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, WardMapperImpl.class })
class WardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WardRepository wardRepository;

    @MockitoBean
    private GisWardRepository gisWardRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void whenUnauthenticated_thenRejectWardRequests() throws Exception {
        mockMvc.perform(get("/api/wards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenAuthenticated_thenReturnWards() throws Exception {
        Province province = Province.builder().code("64").fullName("Tỉnh Gia Lai").build();
        Ward ward = Ward.builder().code("24124").name("Xã An Phú").fullName("Xã An Phú").province(province).build();

        Mockito.when(wardRepository.findAll()).thenReturn(Collections.singletonList(ward));

        mockMvc.perform(get("/api/wards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("24124"))
                .andExpect(jsonPath("$[0].name").value("Xã An Phú"))
                .andExpect(jsonPath("$[0].provinceName").value("Tỉnh Gia Lai"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetWardDetail_thenReturnDetail() throws Exception {
        Province province = Province.builder().code("64").fullName("Tỉnh Gia Lai").build();
        Ward ward = Ward.builder().code("24124").name("Xã An Phú").fullName("Xã An Phú").province(province).build();
        GisWard gisWard = GisWard.builder().areaKm2(new BigDecimal("12.34")).build();

        Mockito.when(wardRepository.findById("24124")).thenReturn(Optional.of(ward));
        Mockito.when(gisWardRepository.findByWardCode("24124")).thenReturn(Optional.of(gisWard));

        mockMvc.perform(get("/api/wards/24124"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("24124"))
                .andExpect(jsonPath("$.areaKm2").value(12.34));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetWardGeoJson_thenReturnGeoJson() throws Exception {
        String mockGeoJson = "{\"type\": \"Polygon\", \"coordinates\": []}";
        Mockito.when(gisWardRepository.findGeoJsonByWardCode("24124")).thenReturn(Optional.of(mockGeoJson));

        mockMvc.perform(get("/api/wards/24124/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Polygon"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetAllWardsGeoJson_thenReturnFeatureCollection() throws Exception {
        Object[] row = new Object[] {
                "24124", "Xã An Phú", "Xã An Phú",
                new BigDecimal("12.34"),
                "{\"type\":\"Polygon\",\"coordinates\":[]}"
        };
        Mockito.when(gisWardRepository.findAllWardsGeoJsonData())
                .thenReturn(Collections.singletonList(row));

        mockMvc.perform(get("/api/wards/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.features[0].geometry.type").value("Polygon"))
                .andExpect(jsonPath("$.features[0].properties.code").value("24124"))
                .andExpect(jsonPath("$.features[0].properties.fullName").value("Xã An Phú"))
                .andExpect(jsonPath("$.features[0].properties.areaKm2").value(12.34));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetAllWardsGeoJson_thenSkipRowWithMalformedGeometry() throws Exception {
        // Trước bản refactor sang Jackson, geomJson lỗi cú pháp sẽ được nối thẳng vào
        // chuỗi JSON output qua StringBuilder -> làm hỏng luôn cả FeatureCollection.
        // Sau khi parse qua ObjectMapper#readTree, hàng lỗi phải bị bỏ qua một cách an
        // toàn thay vì làm hỏng toàn bộ response.
        Object[] badRow = new Object[] {
                "99999", "Xã Lỗi Dữ Liệu", "Xã Lỗi Dữ Liệu",
                new BigDecimal("1.00"),
                "{not-valid-geojson"
        };
        Mockito.when(gisWardRepository.findAllWardsGeoJsonData())
                .thenReturn(Collections.singletonList(badRow));

        mockMvc.perform(get("/api/wards/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features").isEmpty());
    }
}
