package com.website.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardDetailDto {
    private String code;
    private String name;
    private String fullName;
    private String provinceName;
    private BigDecimal areaKm2;
}
