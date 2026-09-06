package com.website.gis.features.ocop.controller;

import com.website.gis.features.ocop.dto.OcopProductCreateRequest;
import com.website.gis.features.ocop.dto.OcopProductDto;
import com.website.gis.features.ocop.dto.OcopProductUpdateRequest;
import com.website.gis.features.ocop.service.OcopService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ocop")
@ConditionalOnProperty(name = "features.ocop.enabled", havingValue = "true")
public class OcopController {

    private final OcopService ocopService;

    public OcopController(OcopService ocopService) {
        this.ocopService = ocopService;
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOcopGeoJson() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(ocopService.getGeoJson());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<OcopProductDto>> getNearbyOcopProducts(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm) {
        return ResponseEntity.ok(ocopService.getNearby(lat, lng, radiusKm));
    }

    @GetMapping
    public ResponseEntity<Page<OcopProductDto>> getAllOcopProducts(
            @RequestParam(required = false) String wardCode,
            @RequestParam(value = "q", required = false) String query,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ocopService.getAll(wardCode, query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OcopProductDto> getOcopProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(ocopService.getById(id));
    }

    @PostMapping
    public ResponseEntity<OcopProductDto> createOcopProduct(@Valid @RequestBody OcopProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ocopService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OcopProductDto> updateOcopProduct(
            @PathVariable Integer id,
            @Valid @RequestBody OcopProductUpdateRequest request) {
        return ResponseEntity.ok(ocopService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOcopProduct(@PathVariable Integer id) {
        ocopService.delete(id);
        return ResponseEntity.ok(Map.of("message", "OCOP product deleted successfully"));
    }
}
