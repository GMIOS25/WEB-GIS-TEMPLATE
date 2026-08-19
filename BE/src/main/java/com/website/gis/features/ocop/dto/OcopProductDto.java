package com.website.gis.features.ocop.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcopProductDto {
    private Integer id;
    private String name;
    private String productType;
    private String description;
    private String wardCode;
    private String wardName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
}
