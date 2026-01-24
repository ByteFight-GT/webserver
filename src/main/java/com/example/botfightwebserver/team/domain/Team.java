package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.common.domain.AuditableSoftDeletableEntity;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.submission.domain.Submission;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "teams",
        indexes = {
                @Index(name = "uk_teams_competition_name_normalized", columnList = "competition_id,name_normalized", unique = true)
        }
)
public class Team extends AuditableSoftDeletableEntity {
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 50)
    private String nameNormalized;

    @Column(name = "quote")
    private String quote = "Welcome to ByteFight!";

    @Column(name = "join_code", unique = true)
    private String joinCode;

    @Column(name = "display_members", nullable = false)
    private boolean displayMembers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_submission_id")
    private Submission currentSubmission;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "team_type")
    private TeamType type = TeamType.regular;

    public void setName(String name) {
        this.name = name;
        if (name != null) {
            this.nameNormalized = name.trim().toLowerCase();
        }
    }
}
