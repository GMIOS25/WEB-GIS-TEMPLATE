package com.website.gis.core.mapper;

import com.website.gis.core.dto.LeaderDto;
import com.website.gis.core.dto.WardDetailDto;
import com.website.gis.core.dto.WardDto;
import com.website.gis.core.entity.GisWard;
import com.website.gis.core.entity.LocalLeader;
import com.website.gis.core.entity.Ward;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
     * (Ward for the administrative fields, GisWard for areaKm2), so this
     * mapping method takes two source parameters. MapStruct matches each
     * @Mapping's "source" prefix (ward.* / gisWard.*) to the correct
     * parameter automatically.
     *
     * gisWard may be null (a ward with no GIS row yet) — MapStruct generates
     * a null check for the whole gisWard.areaKm2 path automatically, so no
     * manual ternary is needed here (unlike the previous hand-written code).
     */
    @Mapping(source = "ward.province.fullName", target = "provinceName")
    @Mapping(source = "gisWard.areaKm2", target = "areaKm2")
    WardDetailDto toDetailDto(Ward ward, GisWard gisWard);

    // --- Not wired into WardDetailDto yet (see WardDto/WardDetailDto's
    // commented-out `leaders` field and API_CONTRACT.md's "Planned shape"
    // note) — kept here so wiring it in later is a one-line change:
    // add `List<LeaderDto> leaders;` to WardDetailDto, add
    // `@Mapping(target = "leaders", source = "leaders")` above, and pass
    // the ward's leaders into toDetailDto from WardController.
    LeaderDto toLeaderDto(LocalLeader leader);

    List<LeaderDto> toLeaderDtos(List<LocalLeader> leaders);
}
