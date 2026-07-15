package com.website.gis.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardDto {
    private String code;
    private String name;
    private String fullName;
    private String provinceName;
    // private List<LeaderDto> leaders;
}
