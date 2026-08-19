package com.website.gis.features.agriculture.mapper;

import com.website.gis.features.agriculture.dto.AgricultureUnitDto;
import com.website.gis.features.agriculture.entity.AgricultureUnit;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface AgricultureUnitMapper {

    @Mapping(target = "wardCode", source = "ward.code")
    @Mapping(target = "wardName", source = "ward.name")
    @Mapping(target = "latitude", source = "geom", qualifiedByName = "pointToLatitude")
    @Mapping(target = "longitude", source = "geom", qualifiedByName = "pointToLongitude")
    AgricultureUnitDto toDto(AgricultureUnit unit);

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
