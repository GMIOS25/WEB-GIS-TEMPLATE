package com.website.gis.core.controller;

import com.website.gis.config.SecurityConfig;
import com.website.gis.config.TestMapperConfig;
import com.website.gis.core.dto.LeaderDto;
import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.security.*;
import com.website.gis.core.service.WardService;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WardController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, TestMapperConfig.class, RestAccessDeniedHandler.class,
        RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class WardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WardService wardService;

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
        WardDto wardDto = WardDto.builder()
                .code("24124")
                .name("Xã An Phú")
                .fullName("Xã An Phú")
                .provinceName("Tỉnh Gia Lai")
                .build();

        Mockito.when(wardService.getWards(null)).thenReturn(Collections.singletonList(wardDto));

        mockMvc.perform(get("/api/wards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("24124"))
                .andExpect(jsonPath("$[0].name").value("Xã An Phú"))
                .andExpect(jsonPath("$[0].provinceName").value("Tỉnh Gia Lai"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetWardDetail_withNoLeaders_thenReturnEmptyLeadersArray() throws Exception {
        WardDetailDto detailDto = WardDetailDto.builder()
                .code("24124")
                .name("Xã An Phú")
                .fullName("Xã An Phú")
                .areaKm2(new BigDecimal("12.34"))
                .leaders(Collections.emptyList())
                .build();

        Mockito.when(wardService.getWardDetail("24124")).thenReturn(detailDto);

        mockMvc.perform(get("/api/wards/24124"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("24124"))
                .andExpect(jsonPath("$.areaKm2").value(12.34))
                .andExpect(jsonPath("$.leaders").isArray())
                .andExpect(jsonPath("$.leaders").isEmpty());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetWardDetail_withLeaders_thenReturnLeadersList() throws Exception {
        LeaderDto leaderDto = LeaderDto.builder()
                .fullName("Nguyễn Văn A")
                .position("Chủ tịch UBND")
                .phoneNumber("0905123456")
                .build();

        WardDetailDto detailDto = WardDetailDto.builder()
                .code("24124")
                .name("Xã An Phú")
                .fullName("Xã An Phú")
                .areaKm2(new BigDecimal("12.34"))
                .leaders(List.of(leaderDto))
                .build();

        Mockito.when(wardService.getWardDetail("24124")).thenReturn(detailDto);

        mockMvc.perform(get("/api/wards/24124"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("24124"))
                .andExpect(jsonPath("$.leaders[0].fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.leaders[0].position").value("Chủ tịch UBND"))
                .andExpect(jsonPath("$.leaders[0].phoneNumber").value("0905123456"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetWardGeoJson_thenReturnGeoJson() throws Exception {
        String mockGeoJson = "{\"type\": \"Polygon\", \"coordinates\": []}";
        Mockito.when(wardService.getWardGeoJson("24124")).thenReturn(mockGeoJson);

        mockMvc.perform(get("/api/wards/24124/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Polygon"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetAllWardsGeoJson_thenReturnFeatureCollection() throws Exception {
        String mockFeatureCollection = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Polygon", "coordinates": []},
                      "properties": {
                        "code": "24124",
                        "name": "Xã An Phú",
                        "fullName": "Xã An Phú",
                        "areaKm2": 12.34
                      }
                    }
                  ]
                }
                """;

        Mockito.when(wardService.getAllWardsGeoJson()).thenReturn(mockFeatureCollection);

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
    void whenGetAllWardsGeoJson_empty_thenReturnEmptyFeatures() throws Exception {
        String emptyFc = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        Mockito.when(wardService.getAllWardsGeoJson()).thenReturn(emptyFc);

        mockMvc.perform(get("/api/wards/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features").isEmpty());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetProvinceGeoJson_thenReturnGeoJson() throws Exception {
        String mockGeoJson = "{\"type\": \"MultiPolygon\", \"coordinates\": []}";
        Mockito.when(wardService.getProvinceGeoJson()).thenReturn(mockGeoJson);

        mockMvc.perform(get("/api/wards/province/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MultiPolygon"));
    }
}
