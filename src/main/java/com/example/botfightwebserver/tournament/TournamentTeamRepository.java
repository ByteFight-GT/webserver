package com.example.botfightwebserver.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long> {
    Optional<TournamentTeam> getTournamentTeamByChallongeParticipantId(Long challongeParticipantId);
}
