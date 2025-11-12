package com.example.botfightwebserver.permissions.infra;

import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.permissions.domain.Permissions;
import com.example.botfightwebserver.permissions.domain.PermissionsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/permissions")
public class PublicPermissionsController {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "allowNewSubmission",
            "allowSetSubmission",
            "allowLeaveTeam"
    );

    private final PermissionsService permissionsService;

    @GetMapping("/{field}")
    public Map<String, Object> getPermissionField(@PathVariable String field) {
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
        }

        Permissions perms = permissionsService.get();

        BeanWrapper wrapper = new BeanWrapperImpl(perms);
        Object value;
        try {
            value = wrapper.getPropertyValue(field);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read permission field: " + field,
                    e
            );
        }

        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Permission field is null: " + field
            );
        }

        return Map.of(field, value);
    }
}
