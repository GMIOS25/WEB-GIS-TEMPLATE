package com.website.gis.core.service;

import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.entity.GisWard;
import com.website.gis.core.entity.LocalLeader;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.mapper.WardMapper;
import com.website.gis.core.repository.GisWardRepository;
import com.website.gis.core.repository.LocalLeaderRepository;
import com.website.gis.core.repository.WardRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WardService {

    private static final String EMPTY_FEATURE_COLLECTION = "{\"type\":\"FeatureCollection\",\"features\":[]}";

    private final WardRepository wardRepository;
    private final GisWardRepository gisWardRepository;
    private final LocalLeaderRepository localLeaderRepository;
    private final WardMapper wardMapper;

    public WardService(WardRepository wardRepository,
                       GisWardRepository gisWardRepository,
                       LocalLeaderRepository localLeaderRepository,
                       WardMapper wardMapper) {
        this.wardRepository = wardRepository;
        this.gisWardRepository = gisWardRepository;
        this.localLeaderRepository = localLeaderRepository;
        this.wardMapper = wardMapper;
    }

    public List<WardDto> getWards(String query) {
        List<Ward> wards = StringUtils.hasText(query)
                ? wardRepository.findByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(query.trim(), query.trim())
                : wardRepository.findAll();

        return wards.stream().map(wardMapper::toDto).toList();
    }

    public WardDetailDto getWardDetail(@NonNull String code) {
        Ward ward = wardRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + code));

        GisWard gisWard = gisWardRepository.findByWardCode(code).orElse(null);
        List<LocalLeader> leaders = localLeaderRepository.findByWardCode(code);

        return wardMapper.toDetailDto(ward, gisWard, leaders);
    }

    public String getWardGeoJson(String code) {
        return gisWardRepository.findGeoJsonByWardCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("GeoJSON not found for ward code: " + code));
    }

    public String getAllWardsGeoJson() {
        return gisWardRepository.findWardsFeatureCollection()
                .orElse(EMPTY_FEATURE_COLLECTION);
    }

    public String getProvinceGeoJson() {
        return gisWardRepository.findProvinceGeoJson()
                .orElseThrow(() -> new ResourceNotFoundException("Province GeoJSON not found"));
    }
}
