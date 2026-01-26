package org.bytefight.webserver.team.domain;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "team_glicko_history",
        indexes = {
                @Index(name = "idx_team_glicko_history_team_match", columnList = "team_id, game_match_id")
        }
)
public class TeamGlickoHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "ladder", nullable = false, length = 50)
    private String ladder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatch gameMatch;

    @Column(name = "old_glicko", nullable = false, precision = 7, scale = 2)
    private BigDecimal oldGlicko;

    @Column(name = "new_glicko", nullable = false, precision = 7, scale = 2)
    private BigDecimal newGlicko;
}
