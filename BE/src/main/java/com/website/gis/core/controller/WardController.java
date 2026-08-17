package com.website.gis.core.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.entity.GisWard;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.mapper.WardMapper;
import com.website.gis.core.repository.GisWardRepository;
import com.website.gis.core.repository.WardRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wards")
public class WardController {

    private static final Logger logger = LoggerFactory.getLogger(WardController.class);

    private final WardRepository wardRepository;
    private final GisWardRepository gisWardRepository;
    private final WardMapper wardMapper;
    private final ObjectMapper objectMapper;

    public WardController(WardRepository wardRepository, GisWardRepository gisWardRepository, WardMapper wardMapper,
            ObjectMapper objectMapper) {
        this.wardRepository = wardRepository;
        this.gisWardRepository = gisWardRepository;
        this.wardMapper = wardMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<WardDto>> getWards(@RequestParam(value = "q", required = false) String query) {
        List<Ward> wards;
        if (query != null && !query.trim().isEmpty()) {
            String trimmed = query.trim();
            wards = wardRepository.findByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(trimmed, trimmed);
        } else {
            wards = wardRepository.findAll();
        }

        List<WardDto> dtos = wards.stream()
                .map(wardMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{code}")
    public ResponseEntity<WardDetailDto> getWardDetail(@PathVariable @NonNull String code) {
        Ward ward = wardRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + code));

        GisWard gisWard = gisWardRepository.findByWardCode(code).orElse(null);

        WardDetailDto dto = wardMapper.toDetailDto(ward, gisWard);

        return ResponseEntity.ok(dto);
    }

    @GetMapping(value = "/{code}/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getWardGeoJson(@PathVariable String code) {
        String geoJson = gisWardRepository.findGeoJsonByWardCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("GeoJSON not found for ward code: " + code));

        return ResponseEntity.ok(geoJson);
    }

    /**
     * Trả về toàn bộ 135 xã dưới dạng 1 FeatureCollection.
     *
     * Trước đây hàm này dựng chuỗi JSON bằng tay qua StringBuilder + tự viết
     * escapeJson() cho từng field string. Cách đó hoạt động đúng nhưng dễ vỡ
     * (thêm field mới phải nhớ escape đúng, dễ quên 1 ký tự điều khiển nào đó)
     * và không tận dụng được bộ serializer JSON đã có sẵn (Jackson). Bản dưới
     * dùng ObjectMapper để dựng cây JsonNode: an toàn hơn, không cần tự escape,
     * và geomJson (chuỗi JSON trả về từ ST_AsGeoJSON của PostGIS) được parse
     * lại thành cây con thay vì nối chuỗi thô.
     */
    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getAllWardsGeoJson() {
        List<Object[]> data = gisWardRepository.findAllWardsGeoJsonData();

        ArrayNode features = objectMapper.createArrayNode();
        for (Object[] row : data) {
            String wardCode = (String) row[0];
            String name = (String) row[1];
            String fullName = (String) row[2];
            BigDecimal areaKm2 = (BigDecimal) row[3];
            String geomJson = (String) row[4];

            if (geomJson == null || geomJson.isBlank()) {
                continue;
            }

            JsonNode geometry;
            try {
                geometry = objectMapper.readTree(geomJson);
            } catch (JsonProcessingException e) {
                // Hình học không parse được (dữ liệu hỏng) - bỏ qua xã này thay vì làm
                // hỏng cả FeatureCollection, nhưng log lại để phát hiện dữ liệu lỗi.
                logger.warn("Skipping ward {} - malformed geometry JSON from database", wardCode, e);
                continue;
            }

            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");
            feature.set("geometry", geometry);

            ObjectNode properties = feature.putObject("properties");
            properties.put("code", wardCode);
            properties.put("name", name);
            properties.put("fullName", fullName);
            if (areaKm2 != null) {
                properties.put("areaKm2", areaKm2);
            } else {
                properties.putNull("areaKm2");
            }

            features.add(feature);
        }

        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.set("features", features);

        return ResponseEntity.ok()
                // cachePrivate(): endpoint này yêu cầu đăng nhập (SecurityConfig ->
                // anyRequest().authenticated()). cachePublic() trước đây cho phép shared/
                // proxy cache lưu lại response phía sau auth - dữ liệu ở đây giống nhau cho
                // mọi user nên rủi ro rò rỉ dữ liệu thấp, nhưng đánh dấu "public" cho response
                // đứng sau xác thực vẫn là anti-pattern nên đổi sang private cho đúng ngữ nghĩa
                // (chỉ cache ở trình duyệt của chính người dùng đó, không cho shared cache giữ).
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(featureCollection);
    }

    @GetMapping(value = "/province/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProvinceGeoJson() {
        String geoJson = gisWardRepository.findProvinceGeoJson()
                .orElseThrow(() -> new ResourceNotFoundException("Province GeoJSON not found"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(geoJson);
    }
}