package org.bytefight.webserver.gamematch.domain;

import org.bytefight.webserver.common.domain.AuditableEntity;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.matchmaking.domain.MatchmakingEvent;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "game_matches")
public class GameMatch extends AuditableEntity {

    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(name = "ladder", nullable = false, length = 50)
    private String ladder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_a_id", nullable = false)
    private Submission submissionA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_b_id", nullable = false)
    private Submission submissionB;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_settings", nullable = false)
    private Map<String, Object> matchSettings;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "match_status")
    private MatchStatus status = MatchStatus.scheduling;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reason", nullable = false, columnDefinition = "match_reason")
    private MatchReason reason = MatchReason.other;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matchmaking_event_id")
    private MatchmakingEvent matchmakingEvent;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
