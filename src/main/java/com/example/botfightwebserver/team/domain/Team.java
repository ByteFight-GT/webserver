package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.submission.domain.Submission;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
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
import java.util.*;

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

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

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

    @Builder.Default
    private Integer numberPlayers=1;

    @Column(nullable = false)
    private boolean displayMembers = false;

    @OneToMany(mappedBy = "teamOne")
    @Builder.Default
    @JsonIgnore
    private List<GameMatch> teamOneMatches = new ArrayList<>();

    @OneToMany(mappedBy = "teamTwo")
    @Builder.Default
    @JsonIgnore
    private List<GameMatch> teamTwoMatches = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name="current_submission_id", nullable = true)
    private Submission currentSubmission;

    private String teamCode;

    @Enumerated(EnumType.STRING)
    private TeamType type = TeamType.NORMAL;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    @PrePersist
    public void onCreate() {
        creationDateTime = LocalDateTime.now(clock);
        lastModifiedDate = LocalDateTime.now(clock);
        teamCode = generateCode();
        uuid = UUID.randomUUID();
    }

    private String generateCode() {
        Random random = new Random();
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

    @PreUpdate
    public void onUpdate() {
        lastModifiedDate = LocalDateTime.now(clock);
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Team team)) return false;
        return Objects.equals(uuid, team.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}

