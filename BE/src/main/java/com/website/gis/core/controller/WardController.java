package com.website.gis.core.controller;

import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.entity.GisWard;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.GisWardRepository;
import com.website.gis.core.repository.WardRepository;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wards")
public class WardController {

    private final WardRepository wardRepository;
    private final GisWardRepository gisWardRepository;

    public WardController(WardRepository wardRepository, GisWardRepository gisWardRepository) {
        this.wardRepository = wardRepository;
        this.gisWardRepository = gisWardRepository;
    }

    @GetMapping
    public ResponseEntity<List<WardDto>> getWards(@RequestParam(value = "q", required = false) String query) {
        List<Ward> wards;
        if (query != null && !query.trim().isEmpty()) {
            wards = wardRepository.findByNameContainingIgnoreCase(query.trim());
        } else {
            wards = wardRepository.findAll();
        }

        List<WardDto> dtos = wards.stream()
                .map(ward -> WardDto.builder()
                        .code(ward.getCode())
                        .name(ward.getName())
                        .fullName(ward.getFullName())
                        .provinceName(ward.getProvince() != null ? ward.getProvince().getFullName() : null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{code}")
    public ResponseEntity<WardDetailDto> getWardDetail(@PathVariable String code) {
        Ward ward = wardRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + code));

        GisWard gisWard = gisWardRepository.findByWardCode(code).orElse(null);

        WardDetailDto dto = WardDetailDto.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .fullName(ward.getFullName())
                .provinceName(ward.getProvince() != null ? ward.getProvince().getFullName() : null)
                .areaKm2(gisWard != null ? gisWard.getAreaKm2() : null)
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping(value = "/{code}/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getWardGeoJson(@PathVariable String code) {
        String geoJson = gisWardRepository.findGeoJsonByWardCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("GeoJSON not found for ward code: " + code));

        return ResponseEntity.ok(geoJson);
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllWardsGeoJson() {
        List<Object[]> data = gisWardRepository.findAllWardsGeoJsonData();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[");
        boolean first = true;
        for (Object[] row : data) {
            String wardCode = (String) row[0];
            String name = (String) row[1];
            String fullName = (String) row[2];
            java.math.BigDecimal areaKm2 = (java.math.BigDecimal) row[3];
            String geomJson = (String) row[4];

            if (geomJson == null || geomJson.trim().isEmpty()) {
                continue;
            }

            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }

            sb.append("{\"type\":\"Feature\",\"geometry\":")
                    .append(geomJson)
                    .append(",\"properties\":{")
                    .append("\"code\":\"").append(escapeJson(wardCode)).append("\",")
                    .append("\"name\":\"").append(escapeJson(name)).append("\",")
                    .append("\"fullName\":\"").append(escapeJson(fullName)).append("\",")
                    .append("\"areaKm2\":").append(areaKm2 != null ? areaKm2.toString() : "null")
                    .append("}}");
        }
        sb.append("]}");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(sb.toString());
    }

    @GetMapping(value = "/province/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProvinceGeoJson() {
        String geoJson = gisWardRepository.findProvinceGeoJson()
                .orElseThrow(() -> new ResourceNotFoundException("Province GeoJSON not found"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(geoJson);
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}