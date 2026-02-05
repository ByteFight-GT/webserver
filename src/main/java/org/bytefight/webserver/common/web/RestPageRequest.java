package org.bytefight.webserver.common.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Set;
import java.util.Set;

public class RestPageRequest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Integer page;
    private Integer perPage;
    private String sort;
    private String order;
    private String range;
    private String filter;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getFilterRaw() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Map<String, Object> getFilter() {
        if (filter == null || filter.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(filter, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public Pageable toPageable(
            int defaultPageSize,
            int maxPageSize,
            String defaultSortField,
            Set<String> allowedSortFields
    ) {
        Integer resolvedPage = page;
        Integer resolvedPerPage = perPage;
        String resolvedSort = sort;
        String resolvedOrder = order;

        Range parsedRange = parseRange(range);
        if (parsedRange != null && (resolvedPage == null || resolvedPerPage == null)) {
            long size = parsedRange.size();
            if (size > 0) {
                resolvedPerPage = (int) Math.min(size, maxPageSize);
                resolvedPage = (int) (parsedRange.start() / resolvedPerPage) + 1;
            }
        }

        SortSpec parsedSort = parseSort(sort);
        if (parsedSort != null && (resolvedOrder == null || resolvedOrder.isBlank())) {
            resolvedSort = parsedSort.field();
            resolvedOrder = parsedSort.order();
        }

        return RestPageable.fromRestParams(
                resolvedPage,
                resolvedPerPage,
                resolvedSort,
                resolvedOrder,
                defaultPageSize,
                maxPageSize,
                defaultSortField,
                allowedSortFields
        );
    }

    private static Range parseRange(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int[] range = OBJECT_MAPPER.readValue(value, int[].class);
            if (range.length < 2) {
                return null;
            }
            int start = range[0];
            int end = range[1];
            if (start < 0 || end < start) {
                return null;
            }
            return new Range(start, end);
        } catch (Exception ex) {
            return null;
        }
    }

    private static SortSpec parseSort(String value) {
        if (value == null || value.isBlank() || !value.trim().startsWith("[")) {
            return null;
        }
        try {
            String[] sort = OBJECT_MAPPER.readValue(value, String[].class);
            if (sort.length < 2) {
                return null;
            }
            return new SortSpec(sort[0], sort[1]);
        } catch (Exception ex) {
            return null;
        }
    }

    private record Range(long start, long end) {
        long size() {
            return end - start + 1;
        }
    }

    private record SortSpec(String field, String order) {
    }
}
