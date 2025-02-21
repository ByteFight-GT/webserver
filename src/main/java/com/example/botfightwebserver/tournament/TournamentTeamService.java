package com.example.botfightwebserver.tournament;

import com.example.botfightwebserver.team.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TournamentTeamService {

    private final TournamentTeamRepository tournamentTeamRepository;

    public void savePlayers(List<TournamentTeam> tournamentTeams) {
        tournamentTeamRepository.saveAll(tournamentTeams);
    }

    public Optional<TournamentTeam> getTeamByChallongeId(Long challongeId) {
        Optional<TournamentTeam> tournamentTeam = tournamentTeamRepository.getTournamentTeamByChallongeParticipantId(challongeId);
        if (tournamentTeam.isPresent()) {
            return tournamentTeam;
        }
        return Optional.empty();
    }
}
