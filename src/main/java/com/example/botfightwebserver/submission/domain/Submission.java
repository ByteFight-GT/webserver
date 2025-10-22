package com.example.botfightwebserver.submission.domain;

import com.example.botfightwebserver.team.domain.Team;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table
@Builder
@NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    private UUID storageFileUuid;

    @ManyToOne()
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    private SUBMISSION_VALIDITY submissionValidity;

    @Enumerated(EnumType.STRING)
    private STORAGE_SOURCE source;

    private LocalDateTime createdAt;

    private LocalDateTime validateAt;

    private String name;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    private Boolean isAutoSet;

    @Builder.Default
    private Boolean isDeleted=false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(clock);
        uuid = UUID.randomUUID();
    }

    public void setSubmissionValidity(SUBMISSION_VALIDITY submissionValidity) {
        this.submissionValidity = submissionValidity;
        if (submissionValidity == SUBMISSION_VALIDITY.VALID) {
            validateAt = LocalDateTime.now(clock);
        }
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }
}
