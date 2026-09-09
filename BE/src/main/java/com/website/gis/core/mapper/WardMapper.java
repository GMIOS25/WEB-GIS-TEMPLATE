package com.website.gis.core.mapper;

import com.website.gis.core.dto.LeaderDto;
import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.entity.LocalLeader;
import com.website.gis.core.entity.Ward;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entity -> DTO mapping for the Ward aggregate. Replaces the manual
 * WardDto/WardDetailDto.builder() calls previously written by hand in
 * WardController.
 *
 * componentModel = "spring" registers the generated implementation as a
 * Spring bean, so it can be constructor-injected like any other bean.
 */
@Mapper(componentModel = "spring")
public interface WardMapper {

    @Mapping(source = "province.fullName", target = "provinceName")
    WardDto toDto(Ward ward);

    /**
     * WardDetailDto is assembled from two different entities/repositories
     * (Ward for the administrative fields, a scalar query for areaKm2), so this
     * mapping method takes two source parameters. MapStruct matches each
     * @Mapping's "source" prefix to the correct
     * parameter automatically.
     *
     * Querying only areaKm2 avoids hydrating the large bbox/geom columns merely to
     * build the ward detail response.
     */
    @Mapping(source = "ward.code", target = "code")
    @Mapping(source = "ward.name", target = "name")
    @Mapping(source = "ward.fullName", target = "fullName")
    @Mapping(source = "ward.province.fullName", target = "provinceName")
    @Mapping(source = "areaKm2", target = "areaKm2")
    @Mapping(source = "leaders", target = "leaders")
    WardDetailDto toDetailDto(Ward ward, BigDecimal areaKm2, List<LocalLeader> leaders);

    LeaderDto toLeaderDto(LocalLeader leader);

    List<LeaderDto> toLeaderDtos(List<LocalLeader> leaders);
}

