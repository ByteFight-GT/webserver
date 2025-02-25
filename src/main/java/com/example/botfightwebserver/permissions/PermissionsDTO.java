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
    private Boolean allowRegister;
    private Boolean allowUpdateTeam;
    private Boolean allowUpdateProfile;

    public static PermissionsDTO fromEntity(Permissions permissions) {
        return PermissionsDTO.builder()
            .allowNewSubmission(permissions.getAllowNewSubmission())
            .allowSetSubmission(permissions.getAllowSetSubmission())
            .allowRegister(permissions.getAllowRegister())
            .allowUpdateTeam(permissions.getAllowUpdateTeam())
            .allowUpdateProfile(permissions.getAllowUpdateProfile())
            .build();
    }

    public static Permissions toEntity(PermissionsDTO dto, LocalDateTime localDateTime) {
        return Permissions.builder()
            .allowNewSubmission(dto.allowNewSubmission)
            .allowSetSubmission(dto.allowSetSubmission)
            .allowRegister(dto.allowRegister)
            .allowUpdateProfile(dto.allowUpdateTeam)
            .allowUpdateProfile(dto.allowUpdateProfile)
            .build();
    }
}
