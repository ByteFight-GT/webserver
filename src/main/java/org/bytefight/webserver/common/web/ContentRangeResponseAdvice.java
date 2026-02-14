package org.bytefight.webserver.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.data.domain.Page;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentRangeResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final int DEFAULT_PAGE = 1;
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\]");

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(body instanceof Page<?> pageBody)) {
            return body;
        }

        if (response instanceof ServletServerHttpResponse servletResponse
                && request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            if (!isAdminPath(httpRequest.getRequestURI())) {
                return body;
            }
            String resourceName = resourceNameFromPath(httpRequest.getRequestURI());
            String pageParam = httpRequest.getParameter("page");
            String perPageParam = httpRequest.getParameter("perPage");
            Range range = parseRange(httpRequest.getParameter("range"));
            int page = parsePositiveInt(pageParam, DEFAULT_PAGE);
            int perPage = parsePositiveInt(perPageParam, pageBody.getSize());

            long total = pageBody.getTotalElements();
            int size = pageBody.getNumberOfElements();
            long start = Math.max((long) (page - 1) * perPage, 0);
            long end = size == 0 ? start : start + size - 1;

            if (range != null && isBlank(pageParam) && isBlank(perPageParam)) {
                start = range.start();
                end = size == 0 ? start : start + size - 1;
            }

            String contentRange = String.format("%s %d-%d/%d", resourceName, start, end, total);
            servletResponse.getServletResponse().setHeader("Content-Range", contentRange);
        }

        return pageBody.getContent();
    }

    private static boolean isAdminPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.startsWith("/api/v1/admin/");
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String resourceNameFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "resource";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == trimmed.length() - 1) {
            return "resource";
        }
        return trimmed.substring(lastSlash + 1);
    }

    private static Range parseRange(String range) {
        if (range == null || range.isBlank()) {
            return null;
        }
        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (!matcher.matches()) {
            return null;
        }
        long start = Long.parseLong(matcher.group(1));
        long end = Long.parseLong(matcher.group(2));
        if (end < start) {
            return null;
        }
        return new Range(start, end);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Range(long start, long end) {
    }
}
