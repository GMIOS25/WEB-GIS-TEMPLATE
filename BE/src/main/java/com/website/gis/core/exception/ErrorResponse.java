package com.website.gis.core.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response payload returned by every API error, as
 * mandated by CODING_CONVENTIONS.md (Section 2.1) and API_CONTRACT.md
 * (Section 2).
 */
@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details; // For validation field errors
}