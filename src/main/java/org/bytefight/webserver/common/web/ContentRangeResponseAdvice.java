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

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentRangeResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final int DEFAULT_PAGE = 1;

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
            String resourceName = resourceNameFromPath(httpRequest.getRequestURI());
            int page = parsePositiveInt(httpRequest.getParameter("page"), DEFAULT_PAGE);
            int perPage = parsePositiveInt(httpRequest.getParameter("perPage"), pageBody.getSize());

            long total = pageBody.getTotalElements();
            int size = pageBody.getNumberOfElements();
            long start = Math.max((long) (page - 1) * perPage, 0);
            long end = size == 0 ? start : start + size - 1;

            String contentRange = String.format("%s %d-%d/%d", resourceName, start, end, total);
            servletResponse.getServletResponse().setHeader("Content-Range", contentRange);
        }

        return pageBody.getContent();
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
}
