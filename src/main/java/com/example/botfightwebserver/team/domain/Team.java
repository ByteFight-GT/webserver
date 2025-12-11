package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.competition.domain.Competition;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Entity
@Table(name = "teams")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Indexed
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // matches BIGINT IDENTITY
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "quote")
    private String quote = "Welcome to ByteFight!";

    @Column(name = "join_code")
    private String joinCode;

    @Column(name = "display_members", nullable = false)
    private boolean displayMembers;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "active_submission_id", nullable = true)
    private Submission currentSubmission;

    @Column(name = "team_type")
    private TeamType type = TeamType.REGULAR;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

