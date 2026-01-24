package com.example.botfightwebserver.gameMatch.application;

import com.example.botfightwebserver.gameMatch.domain.*;
import com.example.botfightwebserver.gameMatch.domain.dto.GameMatchDto;
import com.example.botfightwebserver.gameMatch.domain.dto.GameMatchJob;
import com.example.botfightwebserver.gameMatch.infra.GameMatchProperties;
import com.example.botfightwebserver.gameMatch.infra.GameMatchRepository;
import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.rabbitMQ.application.RabbitMQService;
import com.example.botfightwebserver.submission.application.SubmissionService;
import com.example.botfightwebserver.team.domain.dto.StatsDTO;
import com.example.botfightwebserver.team.application.TeamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameMatchService {

    private final GameMatchRepository gameMatchRepository;
    private final TeamService teamService;
    private final SubmissionService submissionService;
    private final RabbitMQService rabbitMQService;
    private final GameMatchProperties gameMatchProperties;
    private final GameMatchLogService gameMatchLogService;
    private final Clock clock;

    public List<GameMatch> getGameMatches() {
        return gameMatchRepository.findAll();
    }

    public GameMatch createMatch(String team1Uuid, String team2Uuid, String submission1Uuid, String submission2Uuid, MatchReason reason) {
        teamService.validateTeams(team1Uuid, team2Uuid);
        submissionService.validateSubmissions(submission1Uuid, submission2Uuid);
        GameMatch gameMatch = new GameMatch();
//        gameMatch.setTeamOne(teamService.getTeamByUuid(UUID.fromString(team1Uuid)).orElseThrow());
//        gameMatch.setTeamTwo(teamService.getTeamByUuid(UUID.fromString(team2Uuid)).orElseThrow());
//        gameMatch.setSubmissionOne(submissionService.getSubmissionByUuid(submission1Uuid));
//        gameMatch.setSubmissionTwo(submissionService.getSubmissionByUuid(submission2Uuid));
//        gameMatch.setStatus(MatchStatus.WAITING);
        gameMatch.setReason(reason);
        return gameMatch;
    }

    public GameMatch queueMatch(GameMatch match) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                GameMatchJob job = GameMatchJob.from(match);
                rabbitMQService.enqueueGameMatchJob(job);
            }
        });

//        match.setQueuedAt(LocalDateTime.now(clock));
//        match.incrementTimesQueued();
        return gameMatchRepository.save(match);
    }

    public void setGameMatchStatus(String gameMatchUuid, MatchStatus status) {
        Optional maybeGameMatch = gameMatchRepository.findByUuid(UUID.fromString(gameMatchUuid));
        if (maybeGameMatch.isEmpty()) {
            throw new IllegalStateException("Failed setting match to" + status + " Game Id doesn't exist" + gameMatchUuid);
        }
        GameMatch gameMatch = (GameMatch) maybeGameMatch.get();
        gameMatch.setStatus(status);
//        if (!MatchStatus.WAITING.equals(gameMatch.getStatus())) {
//            gameMatch.setProcessedAt(LocalDateTime.now(clock));
//        }
//        if (status == MatchStatus.TEAM_ONE_WIN) {
//            gameMatch.setWinningTeam(gameMatch.getTeamOne());
//        } else if (status == MatchStatus.TEAM_TWO_WIN) {
//            gameMatch.setWinningTeam(gameMatch.getTeamTwo());
//        }
        gameMatchRepository.save(gameMatch);
    }

    public GameMatchDto getDTOById(Long id) {
        return GameMatchDto.fromEntity(gameMatchRepository.getReferenceById(id));
    }

    public Optional<GameMatchDto> getDTOByUuid(String uuid) {
        Optional<GameMatch> dto = gameMatchRepository.findByUuid(UUID.fromString(uuid));
        if(dto.isEmpty()) return Optional.empty();

        return Optional.of(GameMatchDto.fromEntity(dto.get()));
    }

    public GameMatch getReferenceById(Long id) {
        return gameMatchRepository.getReferenceById(id);
    }

    public Optional<GameMatch> getReferenceByUuid(String uuid) {
        return gameMatchRepository.findByUuid(UUID.fromString(uuid));
    }

    public boolean isGameMatchIdExist(Long id) {
        return gameMatchRepository.existsById(id);
    }

    public boolean isGameMatchUuidExist(String uuid) {
        return gameMatchRepository.existsByUuid(UUID.fromString(uuid));
    }

    public boolean isGameMatchWaiting(String uuid) {
        return false;
//        return gameMatchRepository.findByUuid(UUID.fromString(uuid)).orElseThrow().getStatus() == MatchStatus.WAITING;
    }

    public List<GameMatchJob> deleteQueuedMatches() {
        List<GameMatchJob> removedMatches = rabbitMQService.deleteGameMatchQueue();
//        for (GameMatchJob job : removedMatches) {
//            setGameMatchStatus(job.gameMatchUuid(), MatchStatus.MANUALLY_FAILED);
//        }
        return removedMatches;
    }

    public List<GameMatchJob> peekQueuedMatches() {
        return rabbitMQService.peekGameMatchQueue();
    }

    public List<GameMatch> getFailedMatches() {
        return null;
//        return gameMatchRepository
//                .findByStatus(MatchStatus.FAILED)
//                .stream()
//                .toList();
    }

    @Transactional
    public void rescheduleStaleMatches(boolean isIgnoreLimit) {
        LocalDateTime thresholdTime = LocalDateTime.now(clock).minusMinutes(gameMatchProperties.getStaleThresholdMinutes());

        // This atomically marks all stale matches as RESCHEDULING and returns their ids
        List<Long> matchesToReschedule = gameMatchRepository.claimAndMarkStaleMatches(thresholdTime);
        log.info("Found {} matches to reschedule", matchesToReschedule.size());
        matchesToReschedule.forEach(id -> rescheduleMatch(id, isIgnoreLimit));
        log.info("Rescheduling completed");
    }

    @Transactional
    public void adminRescheduleMatches(List<Long> matchIds) {
        log.info("Admin {} matches to reschedule", matchIds.size());
        matchIds.forEach(id -> rescheduleMatch(id, true));
        log.info("Rescheduling completed");
    }

    @Transactional
    public GameMatchJob rescheduleMatch(Long gameMatchId, boolean isIgnoreLimit) {
        GameMatch gameMatch = gameMatchRepository.getReferenceById(gameMatchId);
//        Integer timesQueued = gameMatch.getTimesQueued();
//        if (!isIgnoreLimit && timesQueued == 3) {
//            throw new IllegalStateException("Match " + gameMatch.getId() + " has exceeded maximum retry attempts (3)");
//        }
//
//        if(gameMatch.getStatus() != MatchStatus.WAITING) {
//            throw new IllegalArgumentException("Match " + gameMatch.getId() + " cannot be rescheduled.");
//        }
//
//        gameMatch.setQueuedAt(LocalDateTime.now(clock));
//        gameMatch.incrementTimesQueued();
//        gameMatch.setStatus(MatchStatus.WAITING);
        GameMatchJob job = GameMatchJob.from(gameMatch);
        gameMatchRepository.save(gameMatch);
        rabbitMQService.enqueueGameMatchJob(job);
        log.info("rescheduled match {}", job);
        return job;
    }

    public List<GameMatchDto> getAllTeamMatches(String teamUuid) {
        return null;
//        return gameMatchRepository.findTeamMatches(UUID.fromString(teamUuid), List.of(MatchStatus.FAILED)).stream()
//                .filter((match) -> match.getReason() != MatchReason.TOURNAMENT)
//                .map(GameMatchDto::fromEntity)
//                .toList();
    }

    public Page<GameMatchDto> getTeamMatches(String teamUuid, int page, int size) {
//        PageRequest pageable = PageRequest.of(page, size, Sort.by("processedAt").descending());
//        Page<GameMatch> matches = gameMatchRepository.findTeamMatches(UUID.fromString(teamUuid),
//                List.of(MatchStatus.FAILED), List.of(MatchReason.TOURNAMENT), pageable);

//        return matches.map(GameMatchDto::fromEntity);

        return null;
    }

    public Page<GameMatchDto> getTeamMatches(String teamUuid, String otherTeamUuid, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("processedAt").descending());

//        Page<GameMatch> matches = gameMatchRepository.findTeamMatches(
//                UUID.fromString(teamUuid),
//                UUID.fromString(otherTeamUuid),
//                List.of(MatchStatus.FAILED),
//                List.of(MatchReason.TOURNAMENT),
//                pageable);
//
//        return matches.map(GameMatchDto::fromEntity);

        return null;
    }

    private Page<GameMatchDto> processMatches(Page<GameMatch> matches, PageRequest pageable) {
//        List<GameMatchDto> filteredMatches = matches.getContent()
//                .stream()
//                .filter(match -> match.getReason() != MatchReason.TOURNAMENT)
//                .map(GameMatchDto::fromEntity)
//                .toList();
//
//        return new PageImpl<>(filteredMatches, pageable, matches.getTotalElements());

        return null;
    }

    public StatsDTO getTeamStatsByMatchReason(Long teamId, MatchReason reason) {
        List<GameMatch> matches = gameMatchRepository.findTeamMatchesByReason(teamId, List.of(reason));
        int wins = 0;
        int losses = 0;
        int draws = 0;
//        for (GameMatch match : matches) {
//            if (reason == MatchReason.SCRIMMAGE && match.getTeamOne().equals(match.getTeamTwo())) {
//                continue;
//            }
//            MatchStatus status = match.getStatus();
//            boolean isTeamOne = match.getTeamOne().getId().equals(teamId);
//            if (status == MatchStatus.DRAW) {
//                draws++;
//            } else if ((isTeamOne && status == MatchStatus.TEAM_ONE_WIN) ||
//                    (!isTeamOne && status == MatchStatus.TEAM_TWO_WIN)) {
//                wins++;
//            } else if ((isTeamOne && status == MatchStatus.TEAM_TWO_WIN) ||
//                    (!isTeamOne && status == MatchStatus.TEAM_ONE_WIN)) {
//                losses++;
//            }
//        }

        return StatsDTO.builder()
                .numWins(wins)
                .numLosses(losses)
                .numDraws(draws)
                .matchReason(reason)
                .build();
    }
}
