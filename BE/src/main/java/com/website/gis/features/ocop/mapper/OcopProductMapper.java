package com.website.gis.features.ocop.mapper;

import com.website.gis.features.ocop.dto.OcopProductDto;
import com.website.gis.features.ocop.entity.OcopProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OcopProductMapper {

    @Mapping(source = "ward.code", target = "wardCode")
    @Mapping(source = "ward.fullName", target = "wardName")
    @Mapping(source = "product", target = "latitude", qualifiedByName = "mapLatitude")
    @Mapping(source = "product", target = "longitude", qualifiedByName = "mapLongitude")
    OcopProductDto toDto(OcopProduct product);

    @Named("mapLatitude")
    default BigDecimal mapLatitude(OcopProduct product) {
        if (product == null || product.getGeom() == null) {
            return null;
        }
        return BigDecimal.valueOf(product.getGeom().getY());
    }

    @Named("mapLongitude")
    default BigDecimal mapLongitude(OcopProduct product) {
        if (product == null || product.getGeom() == null) {
            return null;
        }
        return BigDecimal.valueOf(product.getGeom().getX());
    }
}
