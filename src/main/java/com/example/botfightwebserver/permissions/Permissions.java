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

    private Boolean allowNewSubmission;

    private Boolean allowSetSubmission;

    private Boolean allowRegister;

    private Boolean allowUpdateTeam;

    private Boolean allowUpdateProfile;

    private Boolean allowCreateTeam;

    private Boolean allowJoinTeam;

    private LocalDateTime createdAt;
}
