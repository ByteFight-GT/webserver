package com.example.botfightwebserver.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long challongeId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer numPlayers;

    @Builder.Default
    private Integer currentRound = 0;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TOURNAMENT_STATUS status = TOURNAMENT_STATUS.WAITING;

    @Enumerated(EnumType.STRING)
    private TOURNAMENT_TYPE tournamentType;
}
