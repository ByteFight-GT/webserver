package com.example.botfightwebserver.tournament;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TournamentSet {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer teamOneScore;

    private Integer teamTwoScore;

    private Integer round;

    private String challongeMatchId;

    private Long challongePlayer1Id;

    private Long challongePlayer2Id;

    private Long winnerId;

    private TOURNAMENT_SET_STATES state;

    @OneToMany(mappedBy = "tournamentSet")
    @Builder.Default
    @JsonIgnore
    private List<TournamentGameMatch> matches = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;
}
