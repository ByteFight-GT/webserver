package com.example.botfightwebserver.tournament;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamDTO;
import com.example.botfightwebserver.team.TeamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tournament")
@RequiredArgsConstructor
@Transactional
public class TournamentController {

    private final TournamentService tournamentService;
    private final TeamService teamService;
    private final ClockConfig clockConfig;
    private final GameMatchService gameMatchService;
    private final TournamentGameMatchService tournamentGameMatchService;
    private final TournamentSetService tournamentSetService;
    private final TournamentTeamService tournamentTeamService;

    @PostMapping("/create")
    public ResponseEntity<Tournament> createTournament(@RequestParam String name, @RequestParam Integer numberPlayers,
                                                       @RequestParam String description, @RequestParam String type) {
        Tournament tournament = Tournament.builder().name(name).numPlayers(numberPlayers).description(description)
            .tournamentType(TOURNAMENT_TYPE.fromChallongeType(type)).createdAt(LocalDateTime.now(clockConfig.clock()))
            .build();
        return ResponseEntity.ok(tournamentService.createTournament(tournament));
    }

    @PostMapping("/add-eligible-players/{tournamentId}")
    public ResponseEntity<List<TeamDTO>> addPlayers(@PathVariable Long tournamentId) {
        List<Team> eligibleTeams = teamService.getTeamsWithSubmission();
        tournamentService.addPlayers(tournamentId, eligibleTeams);
        return ResponseEntity.ok(eligibleTeams.stream().map(TeamDTO::fromEntity).toList());
    }

    @PostMapping("/start/{tournamentId}")
    public ResponseEntity<Tournament> startTournament(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.startTournament(tournamentId));
    }

    @PostMapping("/finalize/{tournamentId}")
    public ResponseEntity<Tournament> finalizeTournament(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.finalizeTournament(tournamentId));
    }

    @PostMapping("/proceed-round/{tournamentId}")
    public ResponseEntity<List<ChallongeMatchDTO>> proceedRound(@PathVariable Long tournamentId) {
        Tournament tournament = tournamentService.getTournament(tournamentId);

        List<ChallongeMatchDTO> challongeMatches = tournamentService.getTournamentMatches(tournamentId);
        for (ChallongeMatchDTO challongeMatch : challongeMatches) {
            Long player1ChallongeId = challongeMatch.getChallongePlayer1Id();
            Long player2ChallongeId = challongeMatch.getChallongePlayer2Id();

            TournamentTeam tournamentTeam1 = tournamentTeamService.getTeamByChallongeId(player1ChallongeId).orElseThrow(
                () -> new IllegalArgumentException("No team saved with Challonge ID " + player1ChallongeId));
            TournamentTeam tournamentTeam2 = tournamentTeamService.getTeamByChallongeId(player2ChallongeId).orElseThrow(
                () -> new IllegalArgumentException("No team saved with Challonge ID " + player1ChallongeId));

            Team team1 = tournamentTeam1.getTeam();
            Team team2 = tournamentTeam2.getTeam();

            GameMatch
                match =
                gameMatchService.submitGameMatch(team1.getId(), team2.getId(), team2.getCurrentSubmission().getId(),
                    team2.getCurrentSubmission().getId(), MATCH_REASON.TOURNAMENT, "pillars");

            TournamentSet tournamentSet = tournamentSetService.save(TournamentSet.builder()
                .round(challongeMatch.getRound())
                .challongePlayer1Id(challongeMatch.getChallongePlayer1Id())
                .challongePlayer2Id(challongeMatch.getChallongePlayer2Id())
                .challongeMatchId(challongeMatch.getMatchId())
                .state(TOURNAMENT_SET_STATES.PENDING)
                .teamOneScore(0)
                .teamTwoScore(0)
                .build());

            tournamentGameMatchService.save(TournamentGameMatch.builder()
                .gameMatch(match)
                .tournament(tournament)
                .tournamentSet(tournamentSet)
                .build());
        }
        tournament.setCurrentRound(tournament.getCurrentRound() + 1);
        tournamentService.saveTournament(tournament);
        return ResponseEntity.ok(challongeMatches);
    }
}
