package com.example.botfightwebserver.scrimmageMatch;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.team.Team;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ScrimmageMatch {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "match_id")
    private GameMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_team_id", nullable = false)
    private Team initiatorTeam; // The team that queued the scrimmage
}
