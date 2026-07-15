package com.website.gis.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shape for a single local leader entry, nested under {@code WardDetailDto.leaders}
 * once the {@code local_leaders} table is seeded — see API_CONTRACT.md
 * "Planned shape" note under GET /api/wards/{code}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderDto {
    private String fullName;
    private String position;
    private String phoneNumber;
}
