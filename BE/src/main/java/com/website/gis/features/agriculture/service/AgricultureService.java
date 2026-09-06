package com.website.gis.features.agriculture.service;

import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.util.GisPointUtils;
import com.website.gis.features.agriculture.dto.AgricultureUnitCreateRequest;
import com.website.gis.features.agriculture.dto.AgricultureUnitDto;
import com.website.gis.features.agriculture.dto.AgricultureUnitUpdateRequest;
import com.website.gis.features.agriculture.entity.AgricultureUnit;
import com.website.gis.features.agriculture.mapper.AgricultureUnitMapper;
import com.website.gis.features.agriculture.repository.AgricultureUnitRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "features.agriculture.enabled", havingValue = "true")
public class AgricultureService {

    private static final String EMPTY_FEATURE_COLLECTION = "{\"type\":\"FeatureCollection\",\"features\":[]}";

    private final AgricultureUnitRepository agricultureUnitRepository;
    private final WardRepository wardRepository;
    private final AgricultureUnitMapper agricultureUnitMapper;

    public AgricultureService(AgricultureUnitRepository agricultureUnitRepository,
                              WardRepository wardRepository,
                              AgricultureUnitMapper agricultureUnitMapper) {
        this.agricultureUnitRepository = agricultureUnitRepository;
        this.wardRepository = wardRepository;
        this.agricultureUnitMapper = agricultureUnitMapper;
    }

    public Page<AgricultureUnitDto> getAll(String wardCode, Pageable pageable) {
        Page<AgricultureUnit> page = StringUtils.hasText(wardCode)
                ? agricultureUnitRepository.findByWardCode(wardCode, pageable)
                : agricultureUnitRepository.findAll(pageable);

        return page.map(agricultureUnitMapper::toDto);
    }

    public AgricultureUnitDto getById(Integer id) {
        AgricultureUnit unit = agricultureUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị nông nghiệp không tồn tại: " + id));
        return agricultureUnitMapper.toDto(unit);
    }

    public String getGeoJson() {
        return agricultureUnitRepository.findAgricultureFeatureCollection()
                .orElse(EMPTY_FEATURE_COLLECTION);
    }

    public List<AgricultureUnitDto> getNearby(Double lat, Double lng, Double radiusKm) {
        validateNearbyParams(lat, lng, radiusKm);

        List<Integer> nearbyIds = agricultureUnitRepository.findNearbyIds(lat, lng, radiusKm * 1000.0);
        if (nearbyIds.isEmpty()) {
            return List.of();
        }

        List<AgricultureUnit> units = agricultureUnitRepository.findByIdIn(nearbyIds);
        return units.stream().map(agricultureUnitMapper::toDto).toList();
    }

    @Transactional
    public AgricultureUnitDto create(AgricultureUnitCreateRequest request) {
        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));

        Point point = GisPointUtils.createPoint(request.getLatitude(), request.getLongitude());

        AgricultureUnit unit = AgricultureUnit.builder()
                .name(request.getName())
                .unitType(request.getUnitType())
                .description(request.getDescription())
                .ward(ward)
                .geom(point)
                .imageUrl(request.getImageUrl())
                .build();

        AgricultureUnit saved = agricultureUnitRepository.save(unit);
        return agricultureUnitMapper.toDto(saved);
    }

    @Transactional
    public AgricultureUnitDto update(Integer id, AgricultureUnitUpdateRequest request) {
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
            unit.setGeom(GisPointUtils.createPoint(request.getLatitude(), request.getLongitude()));
        }
        if (request.getImageUrl() != null) {
            unit.setImageUrl(request.getImageUrl());
        }

        AgricultureUnit saved = agricultureUnitRepository.save(unit);
        return agricultureUnitMapper.toDto(saved);
    }

    @Transactional
    public void delete(Integer id) {
        AgricultureUnit unit = agricultureUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị nông nghiệp không tồn tại: " + id));
        agricultureUnitRepository.delete(unit);
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
}
