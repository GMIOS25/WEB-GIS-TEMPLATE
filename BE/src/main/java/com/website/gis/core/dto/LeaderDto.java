package com.website.gis.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shape for a single local leader entry, nested under {@code WardDetailDto.leaders}
 * populated from the {@code local_leaders} table (e.g., commune/ward chairperson or vice-chairperson).
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
