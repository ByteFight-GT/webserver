package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import com.example.botfightwebserver.submission.SubmissionService;
import com.google.common.annotations.VisibleForTesting;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GameMatchService {

    private final GameMatchRepository gameMatchRepository;
    private final PlayerService playerService;
    private final SubmissionService submissionService;
    private final RabbitMQService rabbitMQService;
    private final GameMatchLogService gameMatchLogService;
    private final Clock clock;

    @VisibleForTesting
    public static final int STALE_THRESHOLD = 60;


    public List<GameMatch> getGameMatches() {
        return gameMatchRepository.findAll();
    }

    public GameMatch createMatch(Long player1Id, Long player2Id, Long submission1Id, Long submission2Id, MATCH_REASON reason, String map) {
        playerService.validatePlayers(player1Id, player2Id);
        submissionService.validateSubmissions(submission1Id, submission2Id);
        GameMatch gameMatch = new GameMatch();
        gameMatch.setPlayerOne(playerService.getPlayerReferenceById(player1Id));
        gameMatch.setPlayerTwo(playerService.getPlayerReferenceById(player2Id));
        gameMatch.setSubmissionOne(submissionService.getSubmissionReferenceById(submission1Id));
        gameMatch.setSubmissionTwo(submissionService.getSubmissionReferenceById(submission2Id));
        gameMatch.setStatus(MATCH_STATUS.WAITING);
        gameMatch.setReason(reason);
        gameMatch.setMap(map);
        gameMatch.setQueuedAt(LocalDateTime.now(clock));
        gameMatch.setTimesQueued(1);
        return gameMatchRepository.save(gameMatch);
    }

    public GameMatchJob submitGameMatch(Long player1Id, Long player2Id, Long submission1Id, Long submission2Id, MATCH_REASON reason, String map) {
        GameMatch match = createMatch(player1Id, player2Id, submission1Id, submission2Id, reason, map);
        GameMatchJob job = GameMatchJob.fromEntity(match);
        rabbitMQService.enqueueGameMatchJob(job);
        return job;
    }

    public void setGameMatchStatus(Long gameMatchId, MATCH_STATUS status) {
        GameMatch gameMatch = gameMatchRepository.findById(gameMatchId).get();
        gameMatch.setStatus(status);
        if (!MATCH_STATUS.WAITING.equals(gameMatch.getStatus())) {
            gameMatch.setProcessedAt(LocalDateTime.now(clock));
        }
        gameMatchRepository.save(gameMatch);
    }

    //only to be used for testing
    public void submitGameMatchResults(GameMatchResult result) {
        rabbitMQService.enqueueGameMatchResult(result);
    }

    public GameMatchDTO getDTOById(Long id) {
        return GameMatchDTO.fromEntity(gameMatchRepository.getReferenceById(id));
    }

    public GameMatch getReferenceById(Long id) {
        return gameMatchRepository.getReferenceById(id);
    }

    public boolean isGameMatchIdExist(Long id) {
        return gameMatchRepository.existsById(id);
    }

    public boolean isGameMatchWaiting(Long id) {
        return gameMatchRepository.findById(id).get().getStatus() == MATCH_STATUS.WAITING;
    }

    public List<GameMatchJob> deleteQueuedMatches() {
        List<GameMatchJob> removedMatches = rabbitMQService.deleteGameMatchQueue();
        for (GameMatchJob job : removedMatches) {
            setGameMatchStatus(job.gameMatchId(), MATCH_STATUS.MANUALLY_FAILED);
        }
        return removedMatches;
    }

    public List<GameMatchJob> peekQueuedMatches() {
        return rabbitMQService.peekGameMatchQueue();
    }

    public List<GameMatch> getStaleWaitingMatches() {
        LocalDateTime thresholdTime = LocalDateTime.now(clock).minusSeconds(STALE_THRESHOLD);
        return gameMatchRepository
            .findByStatusAndQueuedAtBefore(MATCH_STATUS.WAITING, thresholdTime)
            .stream()
            .toList();
    }

    public List<GameMatch> getFailedMatches() {
        return gameMatchRepository
            .findByStatus(MATCH_STATUS.FAILED)
            .stream()
            .toList();

    }
    }


