package com.website.gis.controller;

import com.website.gis.config.SecurityConfig;
import com.website.gis.core.controller.WardController;
import com.website.gis.core.entity.GisWard;
import com.website.gis.core.entity.Province;
import com.website.gis.core.entity.Ward;
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
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
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
}
