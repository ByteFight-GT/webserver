package com.example.botfightwebserver.team.domain;

import com.example.botfightwebserver.common.domain.BaseEntity;
import com.example.botfightwebserver.common.domain.SoftDeletableEntity;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.player.domain.Player;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "team_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_members_team_player",
                        columnNames = {"team_id", "player_id"}
                ),
                @UniqueConstraint(
                        name = "uk_team_members_competition_player",
                        columnNames = {"competition_id", "player_id"}
                )
        }
)
public class TeamMember extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;
}