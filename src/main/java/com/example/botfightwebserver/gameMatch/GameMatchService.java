package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import com.example.botfightwebserver.submission.SubmissionService;
import com.example.botfightwebserver.team.TeamService;
import com.google.common.annotations.VisibleForTesting;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameMatchService {

    private final GameMatchRepository gameMatchRepository;
    private final TeamService teamService;
    private final SubmissionService submissionService;
    private final RabbitMQService rabbitMQService;
    private final GameMatchLogService gameMatchLogService;
    private final Clock clock;

    @VisibleForTesting
    public static final int STALE_THRESHOLD_MINUTES = 60;


    public List<GameMatch> getGameMatches() {
        return gameMatchRepository.findAll();
    }

    public GameMatch createMatch(Long team1Id, Long team2Id, Long submission1Id, Long submission2Id, MATCH_REASON reason, String map) {
        teamService.validateTeams(team1Id, team2Id);
        submissionService.validateSubmissions(submission1Id, submission2Id);
        GameMatch gameMatch = new GameMatch();
        gameMatch.setTeamOne(teamService.getReferenceById(team1Id));
        gameMatch.setTeamTwo(teamService.getReferenceById(team2Id));
        gameMatch.setSubmissionOne(submissionService.getSubmissionReferenceById(submission1Id));
        gameMatch.setSubmissionTwo(submissionService.getSubmissionReferenceById(submission2Id));
        gameMatch.setStatus(MATCH_STATUS.WAITING);
        gameMatch.setReason(reason);
        gameMatch.setMap(map);
        gameMatch.setQueuedAt(LocalDateTime.now(clock));
        gameMatch.setTimesQueued(1);
        return gameMatchRepository.save(gameMatch);
    }

    public GameMatchJob submitGameMatch(Long team1Id, Long team2Id, Long submission1Id, Long submission2Id, MATCH_REASON reason, String map) {
        GameMatch match = createMatch(team1Id, team2Id, submission1Id, submission2Id, reason, map);
        GameMatchJob job = GameMatchJob.fromEntity(match);
        rabbitMQService.enqueueGameMatchJob(job);
        return job;
    }

    public void setGameMatchStatus(Long gameMatchId, MATCH_STATUS status) {
        System.out.println("GAME MATCHID" + gameMatchId);
        Optional maybeGameMatch = gameMatchRepository.findById(gameMatchId);
        if (maybeGameMatch.isEmpty()) {
            throw new IllegalStateException("Failed setting match to" + status + " Game Id doesn't exist" + gameMatchId);
        }
        GameMatch gameMatch = (GameMatch) maybeGameMatch.get();
        gameMatch.setStatus(status);
        if (!MATCH_STATUS.WAITING.equals(gameMatch.getStatus())) {
            gameMatch.setProcessedAt(LocalDateTime.now(clock));
        }
        gameMatchRepository.save(gameMatch);
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
        LocalDateTime thresholdTime = LocalDateTime.now(clock).minusMinutes(STALE_THRESHOLD_MINUTES);

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

    public List<GameMatchJob> rescheduleFailedAndStaleMatches() {
        List<GameMatch> matchesToReschedule = Stream.concat(getFailedMatches().stream(),
            getStaleWaitingMatches().stream()).toList();
        log.info("Found {} matches to reschedule", matchesToReschedule.size());

        List<GameMatchJob> rescheduledJobs = new ArrayList<>();

        for (GameMatch match : matchesToReschedule) {
            try {
                log.info("Rescheduling match {}", match.getId());
                rescheduledJobs.add(rescheduleMatch(match));
            } catch (Exception e) {
                log.error("Failed to reschedule match {}: {}", match.getId(), e.getMessage());
            }
        }

        log.info("Rescheduling completed");
        return rescheduledJobs;
    }

    public GameMatchJob rescheduleMatch(GameMatch gameMatch) {
        Integer timesQueued = gameMatch.getTimesQueued();
        if (timesQueued == 3) {
            throw new IllegalStateException("Match " + gameMatch.getId() + " has exceeded maximum retry attempts (3)");
        }
        gameMatch.setQueuedAt(LocalDateTime.now(clock));
        gameMatch.setStatus(MATCH_STATUS.WAITING);
        gameMatch.setTimesQueued(gameMatch.getTimesQueued() + 1);
        GameMatchJob job = GameMatchJob.fromEntity(gameMatch);
        rabbitMQService.enqueueGameMatchJob(job);
        gameMatchRepository.save(gameMatch);
        return job;
    }

    public List<GameMatchDTO> getPlayedTeamMatches(Long teamId) {
        return gameMatchRepository.findTeamMatches(teamId, MATCH_STATUS.WAITING).stream().map(GameMatchDTO::fromEntity).toList();
    }

    public Page<GameMatchDTO> getPlayedTeamMatches(Long teamId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("processedAt").descending());
        Page<GameMatchDTO> pageResponse = gameMatchRepository.findTeamMatches(teamId,
            MATCH_STATUS.WAITING, pageable).map(GameMatchDTO::fromEntity);
        return pageResponse;
    }

    public Page<GameMatchDTO> getPlayedTeamMatches(Long teamId, Long otherTeamId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("processedAt").descending());
        System.out.println("Team id " + teamId + " other team Id " + otherTeamId);
        Page<GameMatchDTO> pageResponse =
            gameMatchRepository.findTeamMatches(teamId, otherTeamId, MATCH_STATUS.WAITING, pageable).map(GameMatchDTO::fromEntity);
        return pageResponse;
    }

    }


