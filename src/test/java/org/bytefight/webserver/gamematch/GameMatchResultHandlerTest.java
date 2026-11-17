package org.bytefight.webserver.gamematch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.gamematch.application.GameMatchResultHandler;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.application.GlickoService;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.bytefight.webserver.submission.application.SubmissionService;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.tournament.application.TournamentResultHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameMatchResultHandlerTest {
  @Mock private GameMatchService gameMatchService;
  @Mock private GameMatchRepository gameMatchRepository;
  @Mock private TeamService teamService;
  @Mock private SubmissionService submissionService;
  @Mock private RabbitMQService rabbitMQService;
  @Mock private GlickoService glickoService;
  @Mock private TournamentResultHandler tournamentResultHandler;

  @InjectMocks private GameMatchResultHandler gameMatchResultHandler;

  @Test
  void handleGameMatchResultFinalizesWithRawEngineMetadata() {
    UUID matchUuid = UUID.randomUUID();
    GameMatch gameMatch = new GameMatch();
    gameMatch.setReason(MatchReason.matchmaking);
    GameMatchResult result =
        new GameMatchResult(
            matchUuid.toString(), MatchStatus.team_a_win, "arena_02", "unregistered_reason");

    when(gameMatchRepository.finalizeMatchResult(
            eq(matchUuid),
            eq(MatchStatus.team_a_win),
            any(),
            eq("arena_02"),
            eq("unregistered_reason"),
            eq(List.of(MatchStatus.waiting, MatchStatus.in_progress))))
        .thenReturn(1);
    when(gameMatchService.getGameMatch(matchUuid)).thenReturn(Optional.of(gameMatch));

    gameMatchResultHandler.handleGameMatchResult(result);

    verify(gameMatchRepository)
        .finalizeMatchResult(
            eq(matchUuid),
            eq(MatchStatus.team_a_win),
            any(),
            eq("arena_02"),
            eq("unregistered_reason"),
            eq(List.of(MatchStatus.waiting, MatchStatus.in_progress)));
    verify(glickoService).processGameMatchResult(gameMatch, false);
  }
}
