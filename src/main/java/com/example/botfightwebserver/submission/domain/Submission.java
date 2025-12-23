package com.example.botfightwebserver.submission.domain;

import com.example.botfightwebserver.common.domain.AuditableSoftDeletableEntity;
import com.example.botfightwebserver.storage.domain.FileRecord;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submissions")
public class Submission extends AuditableSoftDeletableEntity {

    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_record_id", nullable = false)
    private FileRecord fileRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "validity", nullable = false, columnDefinition = "submission_validity")
    private SubmissionValidity validity = SubmissionValidity.not_evaluated;
}
