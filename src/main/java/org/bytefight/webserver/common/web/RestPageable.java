package org.bytefight.webserver.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class RestPageable {
    private RestPageable() {
    }

    public static Pageable fromRestParams(
            Integer page,
            Integer perPage,
            String sort,
            String order,
            int defaultPageSize,
            int maxPageSize,
            String defaultSortField,
            Set<String> allowedSortFields
    ) {
        int pageIndex = page == null ? 0 : Math.max(page - 1, 0);
        int size = perPage == null ? defaultPageSize : Math.min(Math.max(perPage, 1), maxPageSize);
        String sortField = normalizeSortField(sort, defaultSortField, allowedSortFields);
        Sort.Direction direction = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageIndex, size, Sort.by(direction, sortField));
    }

    public static Pageable fromRestParams(
            Integer page,
            Integer perPage,
            String sort,
            String order,
            int defaultPageSize,
            int maxPageSize,
            String defaultSortField
    ) {
        return fromRestParams(page, perPage, sort, order, defaultPageSize, maxPageSize, defaultSortField, Set.of());
    }

    private static String normalizeSortField(String sort, String defaultSortField, Set<String> allowedSortFields) {
        String field = (sort == null || sort.isBlank()) ? defaultSortField : sort;
        if (allowedSortFields == null || allowedSortFields.isEmpty()) {
            return field;
        }
        return allowedSortFields.contains(field) ? field : defaultSortField;
    }
}
