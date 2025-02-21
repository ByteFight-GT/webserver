package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.submission.Submission;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    private String map;

    private Integer timesQueued;

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
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }
}

