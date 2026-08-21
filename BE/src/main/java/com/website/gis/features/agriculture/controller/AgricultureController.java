package com.website.gis.features.agriculture.controller;

import com.website.gis.core.entity.Ward;

import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.features.agriculture.dto.AgricultureUnitCreateRequest;
import com.website.gis.features.agriculture.dto.AgricultureUnitDto;
import com.website.gis.features.agriculture.dto.AgricultureUnitUpdateRequest;
import com.website.gis.features.agriculture.entity.AgricultureUnit;
import com.website.gis.features.agriculture.mapper.AgricultureUnitMapper;
import com.website.gis.features.agriculture.repository.AgricultureUnitRepository;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/agriculture")
@ConditionalOnProperty(name = "features.agriculture.enabled", havingValue = "true")
public class AgricultureController {

    private final AgricultureUnitRepository agricultureUnitRepository;
    private final WardRepository wardRepository;
    private final AgricultureUnitMapper agricultureUnitMapper;
    private final GeometryFactory geometryFactory;
    private final ObjectMapper objectMapper;

    public AgricultureController(AgricultureUnitRepository agricultureUnitRepository,
                                 WardRepository wardRepository,
                                 AgricultureUnitMapper agricultureUnitMapper,
                                 ObjectMapper objectMapper) {
        this.agricultureUnitRepository = agricultureUnitRepository;
        this.wardRepository = wardRepository;
        this.agricultureUnitMapper = agricultureUnitMapper;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getAgricultureGeoJson() {
        List<AgricultureUnit> units = agricultureUnitRepository.findAll();

        ArrayNode features = objectMapper.createArrayNode();
        for (AgricultureUnit unit : units) {
            if (unit.getGeom() == null) {
                continue;
            }

            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");

            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "Point");
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(BigDecimal.valueOf(unit.getGeom().getX()));
            coordinates.add(BigDecimal.valueOf(unit.getGeom().getY()));

            ObjectNode properties = feature.putObject("properties");
            properties.put("id", unit.getId());
            properties.put("name", unit.getName());
            if (unit.getUnitType() != null) {
                properties.put("unitType", unit.getUnitType());
            } else {
                properties.putNull("unitType");
            }
            properties.put("wardCode", unit.getWard() != null ? unit.getWard().getCode() : null);
            if (unit.getImageUrl() != null) {
                properties.put("imageUrl", unit.getImageUrl());
            } else {
                properties.putNull("imageUrl");
            }

            features.add(feature);
        }

        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.set("features", features);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(featureCollection);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<AgricultureUnitDto>> getNearbyAgricultureUnits(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm) {

        validateNearbyParams(lat, lng, radiusKm);

        List<AgricultureUnit> units = agricultureUnitRepository.findNearby(lat, lng, radiusKm * 1000.0);
        return ResponseEntity.ok(units.stream().map(agricultureUnitMapper::toDto).toList());
    }

    private static void validateNearbyParams(Double lat, Double lng, Double radiusKm) {
        if (lat == null || lat < -90.0 || lat > 90.0) {
            throw new BadRequestException("Vĩ độ (lat) không hợp lệ (phải từ -90 đến 90)");
        }
        if (lng == null || lng < -180.0 || lng > 180.0) {
            throw new BadRequestException("Kinh độ (lng) không hợp lệ (phải từ -180 đến 180)");
        }
        if (radiusKm == null || radiusKm <= 0.0) {
            throw new BadRequestException("Bán kính (radiusKm) phải lớn hơn 0");
        }
    }

    @GetMapping
    public ResponseEntity<Page<AgricultureUnitDto>> getAll(
            @RequestParam(required = false) String wardCode,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<AgricultureUnit> page;
        if (StringUtils.hasText(wardCode)) {
            page = agricultureUnitRepository.findByWardCode(wardCode, pageable);
        } else {
            page = agricultureUnitRepository.findAll(pageable);
        }

        return ResponseEntity.ok(page.map(agricultureUnitMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgricultureUnitDto> getById(@PathVariable Integer id) {
        AgricultureUnit unit = agricultureUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị nông nghiệp không tồn tại: " + id));
        return ResponseEntity.ok(agricultureUnitMapper.toDto(unit));
    }

    @PostMapping
    public ResponseEntity<AgricultureUnitDto> create(@Valid @RequestBody AgricultureUnitCreateRequest request) {
        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));

        Point point = createPoint(request.getLatitude(), request.getLongitude());

        AgricultureUnit unit = AgricultureUnit.builder()
                .name(request.getName())
                .unitType(request.getUnitType())
                .description(request.getDescription())
                .ward(ward)
                .geom(point)
                .imageUrl(request.getImageUrl())
                .build();

        AgricultureUnit saved = agricultureUnitRepository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(agricultureUnitMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgricultureUnitDto> update(@PathVariable Integer id,
                                                    @Valid @RequestBody AgricultureUnitUpdateRequest request) {
        AgricultureUnit unit = agricultureUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị nông nghiệp không tồn tại: " + id));

        if (StringUtils.hasText(request.getName())) {
            unit.setName(request.getName());
        }
        if (request.getUnitType() != null) {
            unit.setUnitType(request.getUnitType());
        }
        if (request.getDescription() != null) {
            unit.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getWardCode())) {
            Ward ward = wardRepository.findById(request.getWardCode())
                    .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));
            unit.setWard(ward);
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            unit.setGeom(createPoint(request.getLatitude(), request.getLongitude()));
        }
        if (request.getImageUrl() != null) {
            unit.setImageUrl(request.getImageUrl());
        }

        AgricultureUnit saved = agricultureUnitRepository.save(unit);
        return ResponseEntity.ok(agricultureUnitMapper.toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<java.util.Map<String, String>> delete(@PathVariable Integer id) {
        AgricultureUnit unit = agricultureUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị nông nghiệp không tồn tại: " + id));

        agricultureUnitRepository.delete(unit);
        return ResponseEntity.ok(java.util.Map.of("message", "Xóa đơn vị nông nghiệp thành công"));
    }


    private Point createPoint(BigDecimal lat, BigDecimal lng) {
        return geometryFactory.createPoint(new Coordinate(lng.doubleValue(), lat.doubleValue()));
    }
}
