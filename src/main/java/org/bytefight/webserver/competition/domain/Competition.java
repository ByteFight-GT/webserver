package org.bytefight.webserver.competition.domain;

import org.bytefight.webserver.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "competitions")
public class Competition extends BaseEntity {
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "is_whitelisted", nullable = false)
    private boolean isWhitelisted = false;

    @Column(name = "max_players_per_team", nullable = false)
    private int maxPlayersPerTeam;
}