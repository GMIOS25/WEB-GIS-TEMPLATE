package com.website.gis.features.science.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScienceUnitDto {
    private Integer id;
    private String name;
    private String unitType;
    private String description;
    private String wardCode;
    private String wardName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
}
