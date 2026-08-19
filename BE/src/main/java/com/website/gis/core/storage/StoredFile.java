package com.website.gis.core.storage;

public record StoredFile(
        String storedFileName,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String publicUrl
) {}
