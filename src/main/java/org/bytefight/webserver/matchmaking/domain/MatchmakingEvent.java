package org.bytefight.webserver.matchmaking.domain;

import jakarta.persistence.*;
import lombok.*;
import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "matchmaking_events")
public class MatchmakingEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(name = "ladder", nullable = false, length = 50)
    private String ladder;
}
