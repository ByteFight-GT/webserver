package com.example.botfightwebserver.permissions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionsService {

    private final PermissionsRespository permissionsRespository;

    public Permissions createPermission(Permissions permissions) {
        permissionsRespository.save(permissions);
        return permissions;
    }

    public Permissions getLatestPermissions() {
        Permissions latestPermissions = permissionsRespository.findTopByOrderByCreatedAtDesc();
        return latestPermissions;
    }

    public void validateAllowNewSubmission() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowSetSubmission()) {
            throw new IllegalArgumentException("You are not allowed to submit a new submission");
        }
    }

    public void validateAllowSetSubmission() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowSetSubmission()) {
            throw new IllegalArgumentException("You are not allowed to set a new submission");
        }
    }

    public void validateAllowRegister() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowRegister()) {
            throw new IllegalArgumentException("You are not allowed to register");
        }
    }

    public void validateAllowUpdateTeam() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowUpdateTeam()) {
            throw new IllegalArgumentException("You are not allowed to update team");
        }
    }

    public void validateAllowUpdateProfile() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowUpdateProfile()) {
            throw new IllegalArgumentException("You are not allowed to update profile");
        }
    }

    public void validateAllowCreateTeam() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowCreateTeam()) {
            throw new IllegalArgumentException("You are not allowed to create team");
        }
    }

    public void validateAllowJoinTeam() {
        Permissions latest = getLatestPermissions();
        if (!latest.getAllowJoinTeam()) {
            throw new IllegalArgumentException("You are not allowed to join team");
        }
    }
}