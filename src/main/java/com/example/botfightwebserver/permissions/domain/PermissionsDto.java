package com.example.botfightwebserver.permissions.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionsDto {
    private Boolean allowNewSubmission;
    private Boolean allowSetSubmission;
    private Boolean allowRegister;
    private Boolean allowUpdateTeam;
    private Boolean allowUpdateProfile;
    private Boolean allowCreateTeam;
    private Boolean allowJoinTeam;
    private Boolean allowLeaveTeam;
    private Boolean runScheduledMatchmaking;
    private Boolean restrictTeamCreationToStudentEmails;
    private Boolean allowScrimmage;

    public static PermissionsDto fromEntity(Permissions permissions) {
        return PermissionsDto.builder()
            .allowNewSubmission(permissions.getAllowNewSubmission())
            .allowSetSubmission(permissions.getAllowSetSubmission())
            .allowRegister(permissions.getAllowRegister())
            .allowUpdateTeam(permissions.getAllowUpdateTeam())
            .allowUpdateProfile(permissions.getAllowUpdateProfile())
            .allowCreateTeam(permissions.getAllowCreateTeam())
            .allowJoinTeam(permissions.getAllowJoinTeam())
            .allowLeaveTeam(permissions.getAllowLeaveTeam())
            .runScheduledMatchmaking(permissions.getRunScheduledMatchmaking())
            .restrictTeamCreationToStudentEmails(permissions.getRestrictTeamCreationToStudentEmails())
            .allowScrimmage(permissions.getAllowScrimmage())
            .build();
    }

    public void applyToEntity(Permissions entity) {
        entity.setAllowNewSubmission(this.getAllowNewSubmission());
        entity.setAllowSetSubmission(this.getAllowSetSubmission());
        entity.setAllowRegister(this.getAllowRegister());
        entity.setAllowUpdateTeam(this.getAllowUpdateTeam());
        entity.setAllowUpdateProfile(this.getAllowUpdateProfile());
        entity.setAllowCreateTeam(this.getAllowCreateTeam());
        entity.setAllowJoinTeam(this.getAllowJoinTeam());
        entity.setAllowLeaveTeam(this.getAllowLeaveTeam());
        entity.setRunScheduledMatchmaking(this.getRunScheduledMatchmaking());
        entity.setRestrictTeamCreationToStudentEmails(this.getRestrictTeamCreationToStudentEmails());
        entity.setAllowScrimmage(this.getAllowScrimmage());
    }
}
