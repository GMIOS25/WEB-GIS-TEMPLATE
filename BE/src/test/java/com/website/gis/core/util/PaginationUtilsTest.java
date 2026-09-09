package com.website.gis.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationUtilsTest {

    @Test
    void capsPageSizeAndKeepsAllowedSort() {
        Pageable input = PageRequest.of(2, 500, Sort.by(Sort.Direction.DESC, "name"));

        Pageable sanitized = PaginationUtils.sanitize(input, Set.of("id", "name"));

        assertEquals(2, sanitized.getPageNumber());
        assertEquals(100, sanitized.getPageSize());
        assertEquals(Sort.Direction.DESC, sanitized.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void replacesUnsupportedSortWithStableIdSort() {
        Pageable input = PageRequest.of(0, 20, Sort.by("geom"));

        Pageable sanitized = PaginationUtils.sanitize(input, Set.of("id", "name"));

        assertEquals(Sort.Direction.ASC, sanitized.getSort().getOrderFor("id").getDirection());
        assertEquals(null, sanitized.getSort().getOrderFor("geom"));
    }
}
