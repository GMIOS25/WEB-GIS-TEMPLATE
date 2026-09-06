package com.website.gis.core.controller;

import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.service.WardService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/wards")
public class WardController {

    private final WardService wardService;

    public WardController(WardService wardService) {
        this.wardService = wardService;
    }

    @GetMapping
    public ResponseEntity<List<WardDto>> getWards(@RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(wardService.getWards(query));
    }

    @GetMapping("/{code}")
    public ResponseEntity<WardDetailDto> getWardDetail(@PathVariable @NonNull String code) {
        return ResponseEntity.ok(wardService.getWardDetail(code));
    }

    @GetMapping(value = "/{code}/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getWardGeoJson(@PathVariable String code) {
        return ResponseEntity.ok(wardService.getWardGeoJson(code));
    }

    /**
     * Trả về toàn bộ 135 xã dưới dạng 1 FeatureCollection.
     * Toàn bộ cấu trúc GeoJSON đã được PostgreSQL / PostGIS tự động biên soạn
     * thông qua view v_wards_geojson, không tốn tài nguyên Java heap để dựng cây Jackson.
     */
    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllWardsGeoJson() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(wardService.getAllWardsGeoJson());
    }

    @GetMapping(value = "/province/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProvinceGeoJson() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(wardService.getProvinceGeoJson());
    }
}