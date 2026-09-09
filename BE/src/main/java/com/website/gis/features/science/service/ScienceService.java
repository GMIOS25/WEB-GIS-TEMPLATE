package com.website.gis.features.science.service;

import com.website.gis.config.CacheConfig;
import com.website.gis.core.entity.Ward;
import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.WardRepository;
import com.website.gis.core.util.GisPointUtils;
import com.website.gis.core.util.PaginationUtils;
import com.website.gis.features.science.dto.ScienceUnitCreateRequest;
import com.website.gis.features.science.dto.ScienceUnitDto;
import com.website.gis.features.science.dto.ScienceUnitUpdateRequest;
import com.website.gis.features.science.entity.ScienceUnit;
import com.website.gis.features.science.mapper.ScienceUnitMapper;
import com.website.gis.features.science.repository.ScienceUnitRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "features.science.enabled", havingValue = "true")
public class ScienceService {

    private static final String EMPTY_FEATURE_COLLECTION = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    private static final int MAX_NEARBY_RESULTS = 100;
    private static final double MAX_NEARBY_RADIUS_KM = 100.0;
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "name", "unitType");

    private final ScienceUnitRepository scienceUnitRepository;
    private final WardRepository wardRepository;
    private final ScienceUnitMapper scienceUnitMapper;

    public ScienceService(ScienceUnitRepository scienceUnitRepository,
                          WardRepository wardRepository,
                          ScienceUnitMapper scienceUnitMapper) {
        this.scienceUnitRepository = scienceUnitRepository;
        this.wardRepository = wardRepository;
        this.scienceUnitMapper = scienceUnitMapper;
    }

    public Page<ScienceUnitDto> getAll(String wardCode, Pageable pageable) {
        pageable = PaginationUtils.sanitize(pageable, ALLOWED_SORTS);
        Page<ScienceUnit> page = StringUtils.hasText(wardCode)
                ? scienceUnitRepository.findByWardCode(wardCode, pageable)
                : scienceUnitRepository.findAll(pageable);

        return page.map(scienceUnitMapper::toDto);
    }

    public ScienceUnitDto getById(Integer id) {
        ScienceUnit unit = scienceUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị khoa học không tồn tại: " + id));
        return scienceUnitMapper.toDto(unit);
    }

    @Cacheable(cacheNames = CacheConfig.SCIENCE_GEOJSON, sync = true)
    public String getGeoJson() {
        return scienceUnitRepository.findScienceFeatureCollection()
                .orElse(EMPTY_FEATURE_COLLECTION);
    }

    public List<ScienceUnitDto> getNearby(Double lat, Double lng, Double radiusKm) {
        validateNearbyParams(lat, lng, radiusKm);

        List<Integer> nearbyIds = scienceUnitRepository.findNearbyIds(
                lat, lng, radiusKm * 1000.0, MAX_NEARBY_RESULTS);
        if (nearbyIds.isEmpty()) {
            return List.of();
        }

        List<ScienceUnit> units = scienceUnitRepository.findByIdIn(nearbyIds);
        Map<Integer, ScienceUnit> unitsById = units.stream()
                .collect(Collectors.toMap(ScienceUnit::getId, Function.identity()));
        return nearbyIds.stream()
                .map(unitsById::get)
                .filter(unit -> unit != null)
                .map(scienceUnitMapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SCIENCE_GEOJSON, allEntries = true)
    public ScienceUnitDto create(ScienceUnitCreateRequest request) {
        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));

        Point point = GisPointUtils.createPoint(request.getLatitude(), request.getLongitude());

        ScienceUnit unit = ScienceUnit.builder()
                .name(request.getName())
                .unitType(request.getUnitType())
                .description(request.getDescription())
                .ward(ward)
                .geom(point)
                .imageUrl(request.getImageUrl())
                .build();

        ScienceUnit saved = scienceUnitRepository.save(unit);
        return scienceUnitMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SCIENCE_GEOJSON, allEntries = true)
    public ScienceUnitDto update(Integer id, ScienceUnitUpdateRequest request) {
        ScienceUnit unit = scienceUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị khoa học không tồn tại: " + id));

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

        ScienceUnit saved = scienceUnitRepository.save(unit);
        return scienceUnitMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SCIENCE_GEOJSON, allEntries = true)
    public void delete(Integer id) {
        ScienceUnit unit = scienceUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị khoa học không tồn tại: " + id));
        scienceUnitRepository.delete(unit);
    }

    private static void validateNearbyParams(Double lat, Double lng, Double radiusKm) {
        if (lat == null || !Double.isFinite(lat) || lat < -90.0 || lat > 90.0) {
            throw new BadRequestException("Vĩ độ (lat) không hợp lệ (phải từ -90 đến 90)");
        }
        if (lng == null || !Double.isFinite(lng) || lng < -180.0 || lng > 180.0) {
            throw new BadRequestException("Kinh độ (lng) không hợp lệ (phải từ -180 đến 180)");
        }
        if (radiusKm == null || !Double.isFinite(radiusKm) || radiusKm <= 0.0) {
            throw new BadRequestException("Bán kính (radiusKm) phải lớn hơn 0");
        }
        if (radiusKm > MAX_NEARBY_RADIUS_KM) {
            throw new BadRequestException("Bán kính (radiusKm) không được vượt quá 100 km");
        }
    }
}
