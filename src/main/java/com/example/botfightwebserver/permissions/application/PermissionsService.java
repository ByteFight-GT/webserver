package com.example.botfightwebserver.permissions.application;

import com.example.botfightwebserver.permissions.domain.PermissionDeniedException;
import com.example.botfightwebserver.permissions.domain.Permissions;
import com.example.botfightwebserver.permissions.domain.PermissionsDto;
import com.example.botfightwebserver.permissions.infra.PermissionsRespository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionsService {

    private final PermissionsRespository permissionsRespository;

    public Permissions get() {
        return permissionsRespository.findById(1L).orElseThrow();
    }

    @Transactional
    public Permissions update(PermissionsDto dto) {
        Permissions entity = get();
        dto.applyToEntity(entity);
        return permissionsRespository.save(entity);
    }

    public void validateAllowNewSubmission() {
        if (!get().getAllowNewSubmission()) {
            throw new PermissionDeniedException("You may not upload new submissions at this time.");
        }
    }

//    public void validateAllowDeleteSubmission() {
//        Permissions latest = getLatestPermissions();
//        if (!latest.getAllowDeleteSubmission()) {
//            throw new IllegalArgumentException("You are not allowed to delete a submission");
//        }
//    }

    public void validateAllowSetSubmission() {
        if (!get().getAllowSetSubmission()) {
            throw new PermissionDeniedException("You may not change your active submission at this time.");
        }
    }

    public void validateAllowRegister() {
        if (!get().getAllowRegister()) {
            throw new IllegalArgumentException("You are not allowed to register");
        }
    }

    public void validateAllowUpdateTeam() {
        if (!get().getAllowUpdateTeam()) {
            throw new PermissionDeniedException("You may not edit your team at this time.");
        }
    }

    public void validateAllowUpdateProfile() {
        if (!get().getAllowUpdateProfile()) {
            throw new IllegalArgumentException("You may not edit your profile at this time.");
        }
    }

    public void validateAllowCreateTeam() {
        if (!get().getAllowCreateTeam()) {
            throw new IllegalArgumentException("You are not allowed to create team");
        }
    }

    public void validateAllowJoinTeam() {
        if (!get().getAllowJoinTeam()) {
            throw new IllegalArgumentException("You are not allowed to join team");
        }
    }

    @PostConstruct
    @Transactional
    public void ensureGlobalPermissionsExists() {
        permissionsRespository.findById(1L).orElseGet(() -> {
            Permissions perms = new Permissions();
            return permissionsRespository.save(perms);
        });
    }
}