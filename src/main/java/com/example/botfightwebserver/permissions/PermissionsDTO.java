package com.example.botfightwebserver.permissions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionsDTO {

    private Boolean allowNewSubmission;
    private Boolean allowSetSubmission;
//    private Boolean allowDeleteSubmission;
    private Boolean allowRegister;
    private Boolean allowUpdateTeam;
    private Boolean allowUpdateProfile;
    private Boolean allowCreateTeam;
    private Boolean allowJoinTeam;

    public static PermissionsDTO fromEntity(Permissions permissions) {
        return PermissionsDTO.builder()
            .allowNewSubmission(permissions.getAllowNewSubmission())
            .allowSetSubmission(permissions.getAllowSetSubmission())
//            .allowDeleteSubmission(permissions.getAllowDeleteSubmission())
            .allowRegister(permissions.getAllowRegister())
            .allowUpdateTeam(permissions.getAllowUpdateTeam())
            .allowUpdateProfile(permissions.getAllowUpdateProfile())
            .allowCreateTeam(permissions.getAllowCreateTeam())
            .allowJoinTeam(permissions.getAllowJoinTeam())
            .build();
    }

    public static Permissions toEntity(PermissionsDTO dto, LocalDateTime localDateTime) {
        return Permissions.builder()
            .allowNewSubmission(dto.allowNewSubmission)
            .allowSetSubmission(dto.allowSetSubmission)
//            .allowDeleteSubmission(dto.allowDeleteSubmission)
            .allowRegister(dto.allowRegister)
            .allowUpdateProfile(dto.allowUpdateTeam)
            .allowUpdateProfile(dto.allowUpdateProfile)
            .allowCreateTeam(dto.allowCreateTeam)
            .allowJoinTeam(dto.allowJoinTeam)
            .createdAt(localDateTime)
            .build();
    }
}
