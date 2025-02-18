package com.example.botfightwebserver.team;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.submission.Submission;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Indexed
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @FullTextField
    private String name;

    private LocalDateTime creationDateTime;

    private LocalDateTime lastModifiedDate;

    @Builder.Default
    private String quote = "Welcome to ByteFight!";

    @Builder.Default
    private Double glicko=1500.0;

    @Builder.Default
    private Double phi=350.0;

    @Builder.Default
    private Double sigma=0.06;

    @Builder.Default
    private Integer matchesPlayed=0;

    @Builder.Default
    private Integer numberWins=0;
    @Builder.Default
    private Integer numberLosses=0;
    @Builder.Default
    private Integer numberDraws=0;

    @OneToMany(mappedBy = "teamOne")
    @Builder.Default
    private List<GameMatch> teamOneMatches = new ArrayList<>();

    @OneToMany(mappedBy = "teamTwo")
    @Builder.Default
    private List<GameMatch> teamTwoMatches = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name="current_submission_id", nullable = true)
    private Submission currentSubmission;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    @PrePersist
    public void onCreate() {
        creationDateTime = LocalDateTime.now(clock);
        lastModifiedDate = LocalDateTime.now(clock);
    }

    @PreUpdate
    public void onUpdate() {
        lastModifiedDate = LocalDateTime.now(clock);
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }
}

