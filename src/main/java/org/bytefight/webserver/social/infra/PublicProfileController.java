package org.bytefight.webserver.social.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.social.application.ProfileService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@Tag(name = "Profile (Public)")
@RestController
@RequestMapping("api/v1/public/profiles")
@RequiredArgsConstructor

public class PublicProfileController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "major";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("major", "year", "createdAt");

    private final ProfileService profileService;

    @GetMapping
    @Operation(
            operationId = "listProfiles",
            summary = "REST endpoint to list profiles"
    )
    public ResponseEntity<?> getProfiles(
            @ModelAttribute RestPageRequest pageRequest,
            @RequestParam(defaultValue = "false") boolean paginated
    ) {
        Map<String, Object> filter = pageRequest.getFilter();

        String username = parseString(filter, "username");
        String major = parseString(filter, "major");
        Integer year = parseInteger(filter, "year");
        String keyword = parseString(filter, "keyword");

        if (paginated) {
            Pageable pageable = pageRequest.toPageable(
                    DEFAULT_PAGE_SIZE,
                    MAX_PAGE_SIZE,
                    DEFAULT_SORT_FIELD,
                    ALLOWED_SORT_FIELDS
            );
            return ResponseEntity.ok(profileService.getProfiles(username, major, year, keyword, pageable));
        }

        return ResponseEntity.ok(profileService.getProfiles(username, major, year, keyword));
    }

    private static String parseString(Map<String, Object> filter, String key) {
        if (filter == null) return null;
        Object value = filter.get(key);
        if (value instanceof String text && !text.isBlank()) return text;
        return null;
    }

    private static Integer parseInteger(Map<String, Object> filter, String key) {
        if (filter == null) return null;
        Object value = filter.get(key);
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text && !text.isBlank()) {
            try { return Integer.parseInt(text); }
            catch (NumberFormatException ex) { return null; }
        }
        return null;
    }
}