package com.website.gis.features.agriculture.controller;

import com.website.gis.features.agriculture.dto.AgricultureUnitCreateRequest;
import com.website.gis.features.agriculture.dto.AgricultureUnitDto;
import com.website.gis.features.agriculture.dto.AgricultureUnitUpdateRequest;
import com.website.gis.features.agriculture.service.AgricultureService;
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
@RequestMapping("/api/agriculture")
@ConditionalOnProperty(name = "features.agriculture.enabled", havingValue = "true")
public class AgricultureController {

    private final AgricultureService agricultureService;

    public AgricultureController(AgricultureService agricultureService) {
        this.agricultureService = agricultureService;
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAgricultureGeoJson() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(agricultureService.getGeoJson());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<AgricultureUnitDto>> getNearbyAgricultureUnits(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm) {
        return ResponseEntity.ok(agricultureService.getNearby(lat, lng, radiusKm));
    }

    @GetMapping
    public ResponseEntity<Page<AgricultureUnitDto>> getAll(
            @RequestParam(required = false) String wardCode,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(agricultureService.getAll(wardCode, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgricultureUnitDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(agricultureService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AgricultureUnitDto> create(@Valid @RequestBody AgricultureUnitCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agricultureService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgricultureUnitDto> update(@PathVariable Integer id,
                                                     @Valid @RequestBody AgricultureUnitUpdateRequest request) {
        return ResponseEntity.ok(agricultureService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        agricultureService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa cơ sở nông nghiệp thành công"));
    }
}
