package com.example.botfightwebserver.permissions.infra;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.permissions.domain.Permissions;
import com.example.botfightwebserver.permissions.domain.PermissionsDto;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
