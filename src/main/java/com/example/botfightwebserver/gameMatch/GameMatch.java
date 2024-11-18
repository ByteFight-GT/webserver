package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.submission.Submission;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GameMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_one_id", nullable = false)
    private Player playerOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_two_id", nullable = false)
    private Player playerTwo;

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

    private String map;

    private Integer timesQueued;

    private static Clock clock = Clock.systemDefaultZone();

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(clock);
        if (status == null) {
            status = MATCH_STATUS.WAITING;
        }
        if (reason == null) {
            reason = MATCH_REASON.UNKNOWN;
        }
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }
}

