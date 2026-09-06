package com.website.gis.features.science.controller;

import com.website.gis.features.science.dto.ScienceUnitCreateRequest;
import com.website.gis.features.science.dto.ScienceUnitDto;
import com.website.gis.features.science.dto.ScienceUnitUpdateRequest;
import com.website.gis.features.science.service.ScienceService;
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
@RequestMapping("/api/science")
@ConditionalOnProperty(name = "features.science.enabled", havingValue = "true")
public class ScienceController {

    private final ScienceService scienceService;

    public ScienceController(ScienceService scienceService) {
        this.scienceService = scienceService;
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getScienceGeoJson() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(scienceService.getGeoJson());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ScienceUnitDto>> getNearbyScienceUnits(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm) {
        return ResponseEntity.ok(scienceService.getNearby(lat, lng, radiusKm));
    }

    @GetMapping
    public ResponseEntity<Page<ScienceUnitDto>> getAll(
            @RequestParam(required = false) String wardCode,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(scienceService.getAll(wardCode, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScienceUnitDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(scienceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ScienceUnitDto> create(@Valid @RequestBody ScienceUnitCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scienceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScienceUnitDto> update(@PathVariable Integer id,
                                                 @Valid @RequestBody ScienceUnitUpdateRequest request) {
        return ResponseEntity.ok(scienceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        scienceService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa đơn vị khoa học thành công"));
    }
}
