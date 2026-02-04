package org.bytefight.webserver.auth.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bytefight.webserver.auth.application.AdminUserService;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.auth.domain.dto.AdminUserDto;
import org.bytefight.webserver.auth.domain.dto.SelfUserDto;
import org.bytefight.webserver.common.web.RestPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Tag(name = "User (Admin)")
@RequestMapping("/api/v1/admin/user")
@RestController
public class AdminUserController {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "email", "isAdmin", "uuid");

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(
            operationId = "adminListAllUsers",
            summary = "REST endpoint to list all users"
    )
    public ResponseEntity<List<AdminUserDto>> listAll(
            @ModelAttribute RestPageRequest pageRequest
    ) {
        Pageable pageable = pageRequest.toPageable(
                DEFAULT_PAGE_SIZE,
                MAX_PAGE_SIZE,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS
        );
        Page<User> usersPage = adminUserService.listUsers(pageable);
        List<AdminUserDto> data = usersPage.stream().map(AdminUserDto::from).toList();

        long total = usersPage.getTotalElements();
        int size = data.size();
        long start = (long) usersPage.getNumber() * usersPage.getSize();
        long end = size == 0 ? start : start + size - 1;
        String contentRange = String.format("users %d-%d/%d", start, end, total);

        return ResponseEntity.ok()
                .header("Content-Range", contentRange)
                .body(data);
    }
}
