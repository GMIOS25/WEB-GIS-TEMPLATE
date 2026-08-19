package com.website.gis.features.ocop.controller;

import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.features.ocop.dto.OcopProductCreateRequest;
import com.website.gis.features.ocop.dto.OcopProductDto;
import com.website.gis.features.ocop.dto.OcopProductUpdateRequest;
import com.website.gis.features.ocop.entity.OcopProduct;
import com.website.gis.features.ocop.mapper.OcopProductMapper;
import com.website.gis.features.ocop.repository.OcopProductRepository;
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

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ocop")
@ConditionalOnProperty(name = "features.ocop.enabled", havingValue = "true")
public class OcopController {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final OcopProductRepository ocopProductRepository;
    private final WardRepository wardRepository;
    private final OcopProductMapper ocopProductMapper;

    public OcopController(OcopProductRepository ocopProductRepository, WardRepository wardRepository,
            OcopProductMapper ocopProductMapper) {
        this.ocopProductRepository = ocopProductRepository;
        this.wardRepository = wardRepository;
        this.ocopProductMapper = ocopProductMapper;
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
            page = ocopProductRepository.findByWardCodeAndNameContainingIgnoreCase(wardCode.trim(), query.trim(), pageable);
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
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + request.getWardCode()));

        Point geom = createPoint(request.getLongitude(), request.getLatitude());

        OcopProduct product = OcopProduct.builder()
                .name(request.getName().trim())
                .productType(request.getProductType())
                .description(request.getDescription())
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
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + request.getWardCode()));

        Point geom = createPoint(request.getLongitude(), request.getLatitude());

        product.setName(request.getName().trim());
        product.setProductType(request.getProductType());
        product.setDescription(request.getDescription());
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

    private static Point createPoint(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return null;
        }
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
    }
}
