package com.example.botfightwebserver.permissions;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Builder.Default
    private Boolean allowNewSubmission = false;

    @Builder.Default
    private Boolean allowSetSubmission = false;

//    @Builder.Default
//    private Boolean allowDeleteSubmission = false;

    @Builder.Default
    private Boolean allowRegister = false;

    @Builder.Default
    private Boolean allowUpdateTeam = false;

    @Builder.Default
    private Boolean allowUpdateProfile = false;

    @Builder.Default
    private Boolean allowCreateTeam = false;

    @Builder.Default
    private Boolean allowJoinTeam = false;

    private LocalDateTime createdAt;
}
