package org.bytefight.webserver.submission.domain;

import org.bytefight.webserver.common.domain.AuditableSoftDeletableEntity;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
