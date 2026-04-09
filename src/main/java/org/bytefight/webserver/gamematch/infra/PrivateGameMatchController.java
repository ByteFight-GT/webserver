package org.bytefight.webserver.gamematch.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bytefight.webserver.common.domain.PermissionDeniedException;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.dto.CreateMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import org.bytefight.webserver.ladder.application.LadderService;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.turnstile.infra.RequireTurnstile;
import org.bytefight.webserver.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Game Match (Private)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match")
public class PrivateGameMatchController {
  private final PlayerService playerService;
  private final TeamService teamService;
  private final LadderService ladderService;
  private final GameMatchService gameMatchService;

  @PostMapping
  @RequireTurnstile
  public ResponseEntity<List<GameMatchDto>> createGameMatch(
      @AuthenticationPrincipal User user, @RequestBody CreateMatchDto createMatchDto) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Team teamA =
        teamService
            .getTeamByUuid(UUID.fromString(createMatchDto.getTeamAUuid()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team A not found"));
    Team teamB =
        teamService
            .getTeamByUuid(UUID.fromString(createMatchDto.getTeamBUuid()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team B not found"));

    Team initiatingTeam;

    if (teamService.isMember(teamA, player)) {
      initiatingTeam = teamA;
    } else if (teamService.isMember(teamB, player)) {
      initiatingTeam = teamB;
    } else {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must be a member of at least one of the teams.");
    }

    if (teamA.getCurrentSubmission() == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Team A does not have a current submission.");
    }

    if (teamB.getCurrentSubmission() == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Team B does not have a current submission.");
    }

    if (teamA.getCompetition() != teamB.getCompetition()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Team A and Team B must belong to the same competition.");
    }

    Competition competition = teamA.getCompetition();

    if (!competition.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition is not active.");
    }

    if (!competition.isAllowCreateUserMatch()) {
      throw new PermissionDeniedException("You may not create matches at this time.");
    }

    Ladder ladder =
        ladderService
            .getLadder(competition, createMatchDto.getLadder())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));

    if (!ladder.isAllowUserMatches()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Ladder does not allow user created matches.");
    }

    // rate limiting
    long currentWaitingMatches =
        gameMatchService.countTeamQueuedMatchesByLadder(initiatingTeam, ladder);

    if (currentWaitingMatches >= ladder.getMaxQueuedPerTeam()) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "You already have the maximum number of matches queued. Please wait for some matches to finish before adding more.");
    }

    int count = createMatchDto.getCount() == null ? 1 : createMatchDto.getCount();
    count = (int) Math.min(ladder.getMaxQueuedPerTeam() - currentWaitingMatches, count);

    List<GameMatch> gameMatches = new ArrayList<>();

    for (int i = 0; i < count; ++i) {
      GameMatch gameMatch =
          gameMatchService.createMatch(
              user,
              initiatingTeam,
              teamA,
              teamB,
              teamA.getCurrentSubmission(),
              teamB.getCurrentSubmission(),
              ladder.getLadder(),
              MatchReason.scrimmage,
              createMatchDto.getMatchSettings(),
              null);

      gameMatchService.scheduleMatch(gameMatch);

      gameMatches.add(gameMatch);
    }

    return ResponseEntity.ok(
        gameMatches.stream().map(GameMatchDto::fromEntity).collect(Collectors.toList()));
  }
}
