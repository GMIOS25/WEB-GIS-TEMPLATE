package com.website.gis.features.ocop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.gis.config.SecurityConfig;
import com.website.gis.core.entity.Province;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.repository.UserRepository;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.security.*;
import com.website.gis.features.ocop.dto.OcopProductCreateRequest;
import com.website.gis.features.ocop.dto.OcopProductUpdateRequest;
import com.website.gis.features.ocop.entity.OcopProduct;
import com.website.gis.features.ocop.mapper.OcopProductMapperImpl;
import com.website.gis.features.ocop.repository.OcopProductRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OcopController.class)
@TestPropertySource(properties = "features.ocop.enabled=true")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, OcopProductMapperImpl.class,
        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, SecurityErrorResponseWriter.class })
class OcopControllerTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OcopProductRepository ocopProductRepository;

    @MockitoBean
    private WardRepository wardRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Ward testWard;
    private OcopProduct testProduct;

    @BeforeEach
    void setUp() {
        Province province = Province.builder().code("52").fullName("Tỉnh Gia Lai").build();
        testWard = Ward.builder().code("21112").name("Ia Kring").fullName("Phường Ia Kring").province(province).build();
        Point point = GF.createPoint(new Coordinate(107.9812, 13.9723));

        testProduct = OcopProduct.builder()
                .id(1)
                .name("Cà phê Robusta Pleiku")
                .productTypes(List.of("Đồ uống", "Nông sản"))
                .starRating(4)
                .contactPhone("0905123456")
                .locationAddress("123 Hùng Vương, Pleiku")
                .ward(testWard)
                .geom(point)
                .imageUrl("https://example.com/coffee.jpg")
                .build();
    }

    @Test
    void whenUnauthenticated_thenRejectRequests() throws Exception {
        mockMvc.perform(get("/api/ocop"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetOcopProducts_asViewer_thenReturnPaginatedList() throws Exception {
        Mockito.when(ocopProductRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testProduct)));

        mockMvc.perform(get("/api/ocop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cà phê Robusta Pleiku"))
                .andExpect(jsonPath("$.content[0].starRating").value(4))
                .andExpect(jsonPath("$.content[0].contactPhone").value("0905123456"))
                .andExpect(jsonPath("$.content[0].wardCode").value("21112"))
                .andExpect(jsonPath("$.content[0].latitude").value(13.9723))
                .andExpect(jsonPath("$.content[0].longitude").value(107.9812))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetOcopGeoJson_asViewer_thenReturnFeatureCollection() throws Exception {
        Mockito.when(ocopProductRepository.findAll()).thenReturn(List.of(testProduct));

        mockMvc.perform(get("/api/ocop/geojson"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.features[0].geometry.type").value("Point"))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(107.9812))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[1]").value(13.9723))
                .andExpect(jsonPath("$.features[0].properties.id").value(1))
                .andExpect(jsonPath("$.features[0].properties.name").value("Cà phê Robusta Pleiku"))
                .andExpect(jsonPath("$.features[0].properties.starRating").value(4))
                .andExpect(jsonPath("$.features[0].properties.productTypes[0]").value("Đồ uống"))
                .andExpect(jsonPath("$.features[0].properties.wardCode").value("21112"))
                .andExpect(jsonPath("$.features[0].properties.imageUrl").value("https://example.com/coffee.jpg"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyOcopProducts_validParams_thenReturnList() throws Exception {
        Mockito.when(ocopProductRepository.findNearbyIds(13.9723, 107.9812, 10000.0))
                .thenReturn(List.of(1));
        Mockito.when(ocopProductRepository.findByIdIn(List.of(1)))
                .thenReturn(List.of(testProduct));

        mockMvc.perform(get("/api/ocop/nearby")
                        .param("lat", "13.9723")
                        .param("lng", "107.9812")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Cà phê Robusta Pleiku"))
                .andExpect(jsonPath("$[0].latitude").value(13.9723))
                .andExpect(jsonPath("$[0].longitude").value(107.9812));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyOcopProducts_emptyResults_thenReturnEmptyList() throws Exception {
        Mockito.when(ocopProductRepository.findNearbyIds(13.9723, 107.9812, 10000.0))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/ocop/nearby")
                        .param("lat", "13.9723")
                        .param("lng", "107.9812")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyOcopProducts_invalidLat_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/ocop/nearby")
                        .param("lat", "95.0")
                        .param("lng", "107.9812")
                        .param("radiusKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyOcopProducts_invalidLng_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/ocop/nearby")
                        .param("lat", "13.9723")
                        .param("lng", "-190.0")
                        .param("radiusKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetNearbyOcopProducts_invalidRadiusKm_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/ocop/nearby")
                        .param("lat", "13.9723")
                        .param("lng", "107.9812")
                        .param("radiusKm", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetOcopProductById_found_thenReturnDto() throws Exception {
        Mockito.when(ocopProductRepository.findById(1)).thenReturn(Optional.of(testProduct));

        mockMvc.perform(get("/api/ocop/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Cà phê Robusta Pleiku"))
                .andExpect(jsonPath("$.starRating").value(4));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenGetOcopProductById_notFound_thenReturn404() throws Exception {
        Mockito.when(ocopProductRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ocop/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void whenCreateOcopProduct_asViewer_thenReturn403() throws Exception {
        OcopProductCreateRequest request = OcopProductCreateRequest.builder()
                .name("Mật ong rừng Gia Lai")
                .wardCode("21112")
                .latitude(new BigDecimal("13.9723"))
                .longitude(new BigDecimal("107.9812"))
                .build();

        mockMvc.perform(post("/api/ocop")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenCreateOcopProduct_asAdmin_thenReturn201() throws Exception {
        OcopProductCreateRequest request = OcopProductCreateRequest.builder()
                .name("Mật ong rừng Gia Lai")
                .productTypes(List.of("Thực phẩm"))
                .starRating(4)
                .contactPhone("0905999888")
                .locationAddress("Ia Kring, Pleiku")
                .wardCode("21112")
                .latitude(new BigDecimal("13.9723"))
                .longitude(new BigDecimal("107.9812"))
                .imageUrl("https://example.com/honey.jpg")
                .build();

        Mockito.when(wardRepository.findById("21112")).thenReturn(Optional.of(testWard));
        Mockito.when(ocopProductRepository.save(any(OcopProduct.class))).thenReturn(testProduct);

        mockMvc.perform(post("/api/ocop")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenCreateOcopProduct_invalidCoordinates_thenReturn400() throws Exception {
        OcopProductCreateRequest request = OcopProductCreateRequest.builder()
                .name("Tên hợp lệ")
                .wardCode("21112")
                .latitude(new BigDecimal("150.0")) // Invalid latitude > 90
                .longitude(new BigDecimal("107.9812"))
                .build();

        mockMvc.perform(post("/api/ocop")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.latitude").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenUpdateOcopProduct_asAdmin_thenReturn200() throws Exception {
        OcopProductUpdateRequest request = OcopProductUpdateRequest.builder()
                .name("Cà phê Robusta Pleiku Chế biến sâu")
                .productTypes(List.of("Đồ uống cao cấp"))
                .starRating(5)
                .contactPhone("0905123456")
                .locationAddress("123 Hùng Vương")
                .wardCode("21112")
                .latitude(new BigDecimal("13.9723"))
                .longitude(new BigDecimal("107.9812"))
                .build();

        Mockito.when(ocopProductRepository.findById(1)).thenReturn(Optional.of(testProduct));
        Mockito.when(wardRepository.findById("21112")).thenReturn(Optional.of(testWard));
        Mockito.when(ocopProductRepository.save(any(OcopProduct.class))).thenReturn(testProduct);

        mockMvc.perform(put("/api/ocop/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenDeleteOcopProduct_asAdmin_thenReturn200() throws Exception {
        Mockito.when(ocopProductRepository.findById(1)).thenReturn(Optional.of(testProduct));

        mockMvc.perform(delete("/api/ocop/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OCOP product deleted successfully"));

        Mockito.verify(ocopProductRepository).delete(testProduct);
    }
}
