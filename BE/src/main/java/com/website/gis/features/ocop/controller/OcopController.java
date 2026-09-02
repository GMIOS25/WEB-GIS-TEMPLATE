package com.website.gis.features.ocop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.util.GisPointUtils;
import com.website.gis.features.ocop.dto.OcopProductCreateRequest;
import com.website.gis.features.ocop.dto.OcopProductDto;
import com.website.gis.features.ocop.dto.OcopProductUpdateRequest;
import com.website.gis.features.ocop.entity.OcopProduct;
import com.website.gis.features.ocop.mapper.OcopProductMapper;
import com.website.gis.features.ocop.repository.OcopProductRepository;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ocop")
@ConditionalOnProperty(name = "features.ocop.enabled", havingValue = "true")
public class OcopController {

    private final OcopProductRepository ocopProductRepository;
    private final WardRepository wardRepository;
    private final OcopProductMapper ocopProductMapper;
    private final ObjectMapper objectMapper;

    public OcopController(OcopProductRepository ocopProductRepository, WardRepository wardRepository,
            OcopProductMapper ocopProductMapper, ObjectMapper objectMapper) {
        this.ocopProductRepository = ocopProductRepository;
        this.wardRepository = wardRepository;
        this.ocopProductMapper = ocopProductMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getOcopGeoJson() {
        List<OcopProduct> products = ocopProductRepository.findAll();

        ArrayNode features = objectMapper.createArrayNode();
        for (OcopProduct product : products) {
            if (product.getGeom() == null) {
                continue;
            }

            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");

            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "Point");
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(BigDecimal.valueOf(product.getGeom().getX()));
            coordinates.add(BigDecimal.valueOf(product.getGeom().getY()));

            ObjectNode properties = feature.putObject("properties");
            properties.put("id", product.getId());
            properties.put("name", product.getName());

            if (product.getProductTypes() != null && !product.getProductTypes().isEmpty()) {
                ArrayNode typesArray = properties.putArray("productTypes");
                product.getProductTypes().forEach(typesArray::add);
                properties.put("productType", product.getProductTypes().getFirst());
            } else {
                properties.putNull("productTypes");
                properties.putNull("productType");
            }

            if (product.getStarRating() != null) {
                properties.put("starRating", product.getStarRating());
            } else {
                properties.putNull("starRating");
            }

            if (product.getContactPhone() != null) {
                properties.put("contactPhone", product.getContactPhone());
            } else {
                properties.putNull("contactPhone");
            }

            if (product.getLocationAddress() != null) {
                properties.put("locationAddress", product.getLocationAddress());
            } else {
                properties.putNull("locationAddress");
            }

            properties.put("wardCode", product.getWard() != null ? product.getWard().getCode() : null);
            properties.put("wardName", product.getWard() != null ? product.getWard().getFullName() : null);

            if (product.getImageUrl() != null) {
                properties.put("imageUrl", product.getImageUrl());
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
    public ResponseEntity<List<OcopProductDto>> getNearbyOcopProducts(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm) {

        validateNearbyParams(lat, lng, radiusKm);

        List<Integer> nearbyIds = ocopProductRepository.findNearbyIds(lat, lng, radiusKm * 1000.0);
        if (nearbyIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<OcopProduct> products = ocopProductRepository.findByIdIn(nearbyIds);
        return ResponseEntity.ok(products.stream().map(ocopProductMapper::toDto).toList());
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
    public ResponseEntity<Page<OcopProductDto>> getOcopProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "wardCode", required = false) String wardCode,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<OcopProduct> page;
        boolean hasQuery = StringUtils.hasText(query);
        boolean hasWardCode = StringUtils.hasText(wardCode);

        if (hasQuery && hasWardCode) {
            page = ocopProductRepository.findByWardCodeAndNameContainingIgnoreCase(wardCode.trim(), query.trim(),
                    pageable);
        } else if (hasWardCode) {
            page = ocopProductRepository.findByWardCode(wardCode.trim(), pageable);
        } else if (hasQuery) {
            page = ocopProductRepository.findByNameContainingIgnoreCase(query.trim(), pageable);
        } else {
            page = ocopProductRepository.findAll(pageable);
        }

        return ResponseEntity.ok(page.map(ocopProductMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OcopProductDto> getOcopProductDetail(@PathVariable Integer id) {
        OcopProduct product = ocopProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCOP product not found with ID: " + id));

        return ResponseEntity.ok(ocopProductMapper.toDto(product));
    }

    @PostMapping
    public ResponseEntity<OcopProductDto> createOcopProduct(@Valid @RequestBody OcopProductCreateRequest request) {
        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));

        Point geom = GisPointUtils.createPoint(request.getLatitude(), request.getLongitude());

        OcopProduct product = OcopProduct.builder()
                .name(request.getName().trim())
                .productTypes(request.getProductTypes())
                .starRating(request.getStarRating())
                .contactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null)
                .locationAddress(request.getLocationAddress() != null ? request.getLocationAddress().trim() : null)
                .ward(ward)
                .geom(geom)
                .imageUrl(request.getImageUrl())
                .build();

        OcopProduct saved = ocopProductRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ocopProductMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OcopProductDto> updateOcopProduct(
            @PathVariable Integer id,
            @Valid @RequestBody OcopProductUpdateRequest request) {

        OcopProduct product = ocopProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCOP product not found with ID: " + id));

        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));

        Point geom = GisPointUtils.createPoint(request.getLatitude(), request.getLongitude());

        product.setName(request.getName().trim());
        product.setProductTypes(request.getProductTypes());
        product.setStarRating(request.getStarRating());
        product.setContactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null);
        product.setLocationAddress(request.getLocationAddress() != null ? request.getLocationAddress().trim() : null);
        product.setWard(ward);
        product.setGeom(geom);
        product.setImageUrl(request.getImageUrl());

        OcopProduct updated = ocopProductRepository.save(product);
        return ResponseEntity.ok(ocopProductMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOcopProduct(@PathVariable Integer id) {
        OcopProduct product = ocopProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCOP product not found with ID: " + id));

        ocopProductRepository.delete(product);
        return ResponseEntity.ok(Map.of("message", "OCOP product deleted successfully"));
    }
}
