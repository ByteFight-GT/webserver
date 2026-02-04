package org.bytefight.webserver.common.web;

import org.springframework.data.domain.Pageable;

import java.util.Set;

public class RestPageRequest {
    private Integer page;
    private Integer perPage;
    private String sort;
    private String order;

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

    public Pageable toPageable(
            int defaultPageSize,
            int maxPageSize,
            String defaultSortField,
            Set<String> allowedSortFields
    ) {
        return RestPageable.fromRestParams(
                page,
                perPage,
                sort,
                order,
                defaultPageSize,
                maxPageSize,
                defaultSortField,
                allowedSortFields
        );
    }
}
