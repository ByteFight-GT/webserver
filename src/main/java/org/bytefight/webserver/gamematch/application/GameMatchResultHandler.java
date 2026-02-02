package org.bytefight.webserver.gamematch.application;


import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gameMatchLogs.GameMatchLogService;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchUpdate;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.application.GlickoService;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.application.SubmissionService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.application.TeamService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
@Slf4j
public class GameMatchResultHandler {
    private final GameMatchService gameMatchService;
    private final GameMatchRepository gameMatchRepository;

    private final TeamService teamService;
    private final SubmissionService submissionService;
    private final RabbitMQService rabbitMQService;
    private final GlickoService glickoService;

    /**
     * This method handles lightweight match status updates emitted by the engine
     * @param gameMatchUpdate
     */
    public void handleGameMatchUpdate(GameMatchUpdate gameMatchUpdate) {
        GameMatch gameMatch = gameMatchService.getGameMatch(UUID.fromString(gameMatchUpdate.getUuid())).orElseThrow();

        if(gameMatchUpdate.isStarted()) {
            gameMatch.setStatus(MatchStatus.in_progress);
            gameMatch.setStartedAt(Instant.now());
            gameMatchRepository.save(gameMatch);
        }
    }

    public void handleGameMatchResult(GameMatchResult result) {
        GameMatch gameMatch = gameMatchService.getGameMatch(UUID.fromString(result.getUuid()))
                .orElseThrow(() -> new IllegalArgumentException("Game match not found"));

        MatchStatus status = gameMatch.getStatus();
        gameMatch.setStatus(status);
        gameMatchRepository.save(gameMatch);

        if(gameMatch.getReason() != MatchReason.validation) {
            glickoService.processGameMatchResult(gameMatch, false);
        }
    }

    public List<GameMatchResult> deleteQueuedMatches() {
        List<GameMatchResult> removedResults = rabbitMQService.deleteGameResultQueue();
        return removedResults;
    }
}
