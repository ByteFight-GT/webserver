package com.example.botfightwebserver.permissions;

import com.example.botfightwebserver.config.ClockConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/permissions")
public class PermissionsController {

    private final PermissionsService permissionsService;
    private final ClockConfig clockConfig;

    @PostMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionsDTO> updatePermissions(@RequestBody PermissionsDTO permissionsDTO) {
        Permissions permissions = PermissionsDTO.toEntity(permissionsDTO, LocalDateTime.now(clockConfig.clock()));
        permissionsService.createPermission(permissions);
        return ResponseEntity.ok(permissionsDTO);
    }

    @GetMapping("/public/get")
    public ResponseEntity<PermissionsDTO> getPermissions() {
        return ResponseEntity.ok(PermissionsDTO.fromEntity(permissionsService.getLatestPermissions()));
    }
}
