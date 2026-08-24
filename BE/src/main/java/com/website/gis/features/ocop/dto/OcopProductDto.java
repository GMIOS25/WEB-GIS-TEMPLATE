package com.website.gis.features.ocop.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcopProductDto {
    private Integer id;
    private String name;
    private List<String> productTypes;
    private Integer starRating;
    private String contactPhone;
    private String locationAddress;
    private String wardCode;
    private String wardName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
}
