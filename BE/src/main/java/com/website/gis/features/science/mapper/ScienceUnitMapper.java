package com.website.gis.features.science.mapper;

import com.website.gis.features.science.dto.ScienceUnitDto;
import com.website.gis.features.science.entity.ScienceUnit;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ScienceUnitMapper {

    @Mapping(target = "wardCode", source = "ward.code")
    @Mapping(target = "wardName", source = "ward.name")
    @Mapping(target = "latitude", source = "geom", qualifiedByName = "pointToLatitude")
    @Mapping(target = "longitude", source = "geom", qualifiedByName = "pointToLongitude")
    ScienceUnitDto toDto(ScienceUnit unit);

    @Named("pointToLatitude")
    default BigDecimal pointToLatitude(Point geom) {
        if (geom == null) {
            return null;
        }
        return BigDecimal.valueOf(geom.getY());
    }

    @Named("pointToLongitude")
    default BigDecimal pointToLongitude(Point geom) {
        if (geom == null) {
            return null;
        }
        return BigDecimal.valueOf(geom.getX());
    }
}
