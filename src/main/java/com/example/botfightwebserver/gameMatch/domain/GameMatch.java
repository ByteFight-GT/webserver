package com.example.botfightwebserver.gameMatch.domain;

import com.example.botfightwebserver.matchMaking.domain.MatchMakingEvent;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.submission.domain.Submission;
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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GameMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_one_id", nullable = false)
    private Team teamOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_two_id", nullable = false)
    private Team teamTwo;

    @ManyToOne()
    @JoinColumn(name = "submission_one_id", nullable = false)
    private Submission submissionOne;

    @ManyToOne()
    @JoinColumn(name = "submission_two_id", nullable = false)
    private Submission submissionTwo;

    @Enumerated(EnumType.STRING)
    private MATCH_STATUS status;

    @Enumerated(EnumType.STRING)
    private MATCH_REASON reason;

    private LocalDateTime createdAt;
    private LocalDateTime queuedAt;
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "matchmaking_event_id", nullable = true)
    private MatchMakingEvent matchmakingEvent;

    @ManyToOne()
    @JoinColumn(name = "winning_team_id", nullable = true)
    private Team winningTeam;

    private String map;

    private Integer timesQueued = 0;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(clock);
        if (status == null) {
            status = MATCH_STATUS.WAITING;
        }
        if (reason == null) {
            reason = MATCH_REASON.UNKNOWN;
        }
        uuid = UUID.randomUUID();
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }

    public void incrementTimesQueued() {
        timesQueued += 1;
    }
}

