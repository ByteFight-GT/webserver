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

    public boolean isAllowed(String action) {
        Permissions permissions = getLatestPermissions();
        if (action == "newSubmission") {
            return permissions.getAllowNewSubmission();
        }
        if (action == "setSubmission") {
            return permissions.getAllowSetSubmission();
        }
        if (action == "register") {
            return permissions.getAllowRegister();
        }
        if (action == "updateTeam") {
            return permissions.getAllowUpdateTeam();
        }
        if (action == "updateProfile") {
            return permissions.getAllowUpdateProfile();
        }
        if (action == "createTeam") {
            return permissions.getAllowCreateTeam();
        }
        if (action == "joinTeam") {
            return permissions.getAllowJoinTeam();
        }

        return false;
    }

}
