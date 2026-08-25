package org.bytefight.webserver.scrim.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.ladder.application.LadderService;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.scrim.application.ScrimRejectedException;
import org.bytefight.webserver.scrim.application.ScrimService;
import org.bytefight.webserver.scrim.domain.ScrimWindow;
import org.bytefight.webserver.scrim.domain.dto.CreateScrimDto;
import org.bytefight.webserver.scrim.domain.dto.ScrimResponseDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.user.domain.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Scrim", description = "Rate-limited server-side practice matches against TA bots")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scrim")
public class ScrimController {
  private final PlayerService playerService;
  private final TeamService teamService;
  private final CompetitionService competitionService;
  private final LadderService ladderService;
  private final ScrimService scrimService;

  @PostMapping
  @Operation(
      operationId = "createScrim",
      summary = "Schedule rate-limited scrim matches against a TA bot")
  public ResponseEntity<ScrimResponseDto> createScrim(
      @AuthenticationPrincipal User user, @RequestBody @Valid CreateScrimDto dto) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(dto.getCompetitionSlug())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));

    if (!competition.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition is not active.");
    }

    // game_matches has a composite FK on (competition, ladder), so the scrim ladder must be
    // provisioned for this competition. New competitions get it automatically; older ones need it
    // created by an admin first.
    if (ladderService.getLadder(competition, DefaultLadders.SCRIM).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Scrim is not available for this competition.");
    }

    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Team student =
        teamService
            .findTeamByCompetitionAndPlayer(competition, player)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "You are not on a team in this competition."));

    if (student.getCurrentSubmission() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Your team has no current submission to scrim with.");
    }

    Team taBot =
        teamService
            .getTeamByCompetitionAndName(competition, dto.getTaBotSlug())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TA bot not found"));

    if (taBot.getType() != TeamType.ta_bot) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That opponent is not a TA bot.");
    }

    if (taBot.getCurrentSubmission() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "That TA bot has no current submission.");
    }

    ScrimProperties props = scrimService.getProperties();
    int requested = dto.getCount() == null ? 1 : Math.max(1, dto.getCount());
    // Never loop past the daily cap; the burst check inside each unit bounds concurrency further.
    requested = Math.min(requested, props.getDailyCap());

    List<GameMatch> scheduled = new ArrayList<>();
    ScrimRejectedException rejection = null;
    for (int i = 0; i < requested; i++) {
      try {
        scheduled.add(scrimService.scheduleOneOrThrow(user, student, taBot, competition));
      } catch (ScrimRejectedException e) {
        rejection = e;
        break;
      }
    }

    if (scheduled.isEmpty()) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header(HttpHeaders.RETRY_AFTER, String.valueOf(rejection.getRetryAfterSeconds()))
          .build();
    }

    ScrimResponseDto body =
        ScrimResponseDto.builder()
            .taBotSlug(dto.getTaBotSlug())
            .scheduled(
                scheduled.stream().map(ScrimResponseDto.ScrimMatchDto::from).toList())
            .remainingDaily(scrimService.remaining(student, ScrimWindow.daily, props.getDailyCap()))
            .remainingWeekly(
                scrimService.remaining(student, ScrimWindow.weekly, props.getWeeklyCap()))
            .build();
    return ResponseEntity.ok(body);
  }
}
