package com.example.botfightwebserver.tournament;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.team.domain.PublicTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.application.TeamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

            GameMatch match = gameMatchService.createMatch(
                    team1.getUuid().toString(),
                    team2.getUuid().toString(),
                    team1.getCurrentSubmission().getUuid().toString(),
                    team2.getCurrentSubmission().getUuid().toString(),
                    MATCH_REASON.TOURNAMENT,
                    TOURNEY_MAP.getRandomMap().toMapName()
            );

            gameMatchService.queueMatch(match);


            TournamentSet tournamentSet = tournamentSetService.save(TournamentSet.builder()
                    .round(challongeMatch.getRound())
                    .challongePlayer1Id(challongeMatch.getChallongePlayer1Id())
                    .challongePlayer2Id(challongeMatch.getChallongePlayer2Id())
                    .challongeMatchId(challongeMatch.getMatchId())
                    .state(TOURNAMENT_SET_STATES.PENDING)
                    .teamOneScore(0)
                    .teamTwoScore(0)
                    .tournament(tournament)
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
