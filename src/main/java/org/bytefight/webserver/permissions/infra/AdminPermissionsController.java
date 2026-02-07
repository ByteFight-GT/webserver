package org.bytefight.webserver.permissions.infra;

import org.bytefight.webserver.permissions.domain.PermissionsDto;
import org.bytefight.webserver.permissions.application.PermissionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/permissions")
public class AdminPermissionsController {

    private final PermissionsService permissionsService;

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public PermissionsDto getPermissions() {
        return PermissionsDto.fromEntity(permissionsService.get());
    }

    @PutMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public PermissionsDto updatePermissions(@RequestBody PermissionsDto dto) {
        return PermissionsDto.fromEntity(permissionsService.update(dto));
    }
}
