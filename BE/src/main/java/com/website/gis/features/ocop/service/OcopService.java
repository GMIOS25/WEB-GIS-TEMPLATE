package com.website.gis.features.ocop.service;

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
@ConditionalOnProperty(name = "features.ocop.enabled", havingValue = "true")
public class OcopService {

    private static final String EMPTY_FEATURE_COLLECTION = "{\"type\":\"FeatureCollection\",\"features\":[]}";

    private final OcopProductRepository ocopProductRepository;
    private final WardRepository wardRepository;
    private final OcopProductMapper ocopProductMapper;

    public OcopService(OcopProductRepository ocopProductRepository,
                       WardRepository wardRepository,
                       OcopProductMapper ocopProductMapper) {
        this.ocopProductRepository = ocopProductRepository;
        this.wardRepository = wardRepository;
        this.ocopProductMapper = ocopProductMapper;
    }

    public Page<OcopProductDto> getAll(String wardCode, String query, Pageable pageable) {
        Page<OcopProduct> page;
        boolean hasWard = StringUtils.hasText(wardCode);
        boolean hasQuery = StringUtils.hasText(query);

        if (hasWard && hasQuery) {
            page = ocopProductRepository.findByWardCodeAndNameContainingIgnoreCase(wardCode, query.trim(), pageable);
        } else if (hasWard) {
            page = ocopProductRepository.findByWardCode(wardCode, pageable);
        } else if (hasQuery) {
            page = ocopProductRepository.findByNameContainingIgnoreCase(query.trim(), pageable);
        } else {
            page = ocopProductRepository.findAll(pageable);
        }

        return page.map(ocopProductMapper::toDto);
    }

    public OcopProductDto getById(Integer id) {
        OcopProduct product = ocopProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCOP product not found with ID: " + id));
        return ocopProductMapper.toDto(product);
    }

    public String getGeoJson() {
        return ocopProductRepository.findOcopFeatureCollection()
                .orElse(EMPTY_FEATURE_COLLECTION);
    }

    public List<OcopProductDto> getNearby(Double lat, Double lng, Double radiusKm) {
        validateNearbyParams(lat, lng, radiusKm);

        List<Integer> nearbyIds = ocopProductRepository.findNearbyIds(lat, lng, radiusKm * 1000.0);
        if (nearbyIds.isEmpty()) {
            return List.of();
        }

        List<OcopProduct> products = ocopProductRepository.findByIdIn(nearbyIds);
        return products.stream().map(ocopProductMapper::toDto).toList();
    }

    @Transactional
    public OcopProductDto create(OcopProductCreateRequest request) {
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
        return ocopProductMapper.toDto(saved);
    }

    @Transactional
    public OcopProductDto update(Integer id, OcopProductUpdateRequest request) {
        OcopProduct product = ocopProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCOP product not found with ID: " + id));

        if (StringUtils.hasText(request.getName())) {
            product.setName(request.getName().trim());
        }
        if (request.getProductTypes() != null) {
            product.setProductTypes(request.getProductTypes());
        }
        if (request.getStarRating() != null) {
            product.setStarRating(request.getStarRating());
        }
        if (request.getContactPhone() != null) {
            product.setContactPhone(request.getContactPhone().trim());
        }
        if (request.getLocationAddress() != null) {
            product.setLocationAddress(request.getLocationAddress().trim());
        }
        if (StringUtils.hasText(request.getWardCode())) {
            Ward ward = wardRepository.findById(request.getWardCode())
                    .orElseThrow(() -> new BadRequestException("Mã xã/phường không tồn tại: " + request.getWardCode()));
            product.setWard(ward);
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            product.setGeom(GisPointUtils.createPoint(request.getLatitude(), request.getLongitude()));
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        OcopProduct saved = ocopProductRepository.save(product);
        return ocopProductMapper.toDto(saved);
    }

    @Transactional
    public void delete(Integer id) {
        OcopProduct product = ocopProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCOP product not found with ID: " + id));
        ocopProductRepository.delete(product);
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
