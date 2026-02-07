package org.bytefight.webserver.common.domain.dto;

import java.util.List;

public record ListResponseDto<T>(List<T> data, long total) {
    public static <T> ListResponseDto<T> of(List<T> data, long total) {
        return new ListResponseDto<>(data, total);
    }
}
