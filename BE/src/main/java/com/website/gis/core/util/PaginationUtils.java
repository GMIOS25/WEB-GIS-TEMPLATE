package com.website.gis.core.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public final class PaginationUtils {

    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSortProperties) {
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        List<Sort.Order> safeOrders = pageable.getSort().stream()
                .filter(order -> allowedSortProperties.contains(order.getProperty()))
                .toList();
        Sort safeSort = safeOrders.isEmpty() ? Sort.by(Sort.Direction.ASC, "id") : Sort.by(safeOrders);
        return PageRequest.of(pageable.getPageNumber(), pageSize, safeSort);
    }
}
