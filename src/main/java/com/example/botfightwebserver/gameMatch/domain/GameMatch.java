package com.example.botfightwebserver.gameMatch.domain;

import com.example.botfightwebserver.common.domain.AuditableEntity;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.matchMaking.domain.MatchmakingEvent;
import com.example.botfightwebserver.submission.domain.Submission;
import com.example.botfightwebserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "match_status")
    private MatchStatus status = MatchStatus.scheduling;

    @Enumerated(EnumType.STRING)
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
