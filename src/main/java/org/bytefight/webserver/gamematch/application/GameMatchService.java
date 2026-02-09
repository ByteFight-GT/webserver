package org.bytefight.webserver.gamematch.application;

import jakarta.persistence.criteria.Predicate;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.infra.GameMatchProperties;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.matchMaking.domain.MatchmakingEvent;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.StatsDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameMatchService {

    private final GameMatchRepository gameMatchRepository;
    private final RabbitMQService rabbitMQService;
    private final GameMatchProperties gameMatchProperties;
    private final Clock clock;

    public Optional<GameMatch> getGameMatch(UUID id) {
        return gameMatchRepository.findByUuid(id);
    }

    public GameMatch createMatch(
            User creatingUser,
            Team teamA,
            Team teamB,
            Submission submissionA,
            Submission submissionB,
            String ladder,
            MatchReason reason,
            MatchmakingEvent matchmakingEvent
    ) {
        if(teamA == null || teamB == null) {
            throw new IllegalArgumentException("teamA and teamB are required");
        }

        if(submissionA == null || submissionB == null) {
            throw new IllegalArgumentException("submissionA and submissionB are required");
        }

        if(!teamA.getCompetition().equals(teamB.getCompetition())) {
            throw new IllegalArgumentException("Both teams must be from the same competition");
        }

        GameMatch gameMatch = new GameMatch();
        gameMatch.setUuid(UUID.randomUUID());
        gameMatch.setCompetition(teamA.getCompetition());
        gameMatch.setTeamA(teamA);
        gameMatch.setTeamB(teamB);
        gameMatch.setSubmissionA(submissionA);
        gameMatch.setSubmissionB(submissionB);
        gameMatch.setMatchSettings(java.util.Collections.emptyMap());
        gameMatch.setStatus(MatchStatus.created);
        gameMatch.setLadder(ladder.trim().toLowerCase());
        gameMatch.setReason(reason);
        gameMatch.setMatchmakingEvent(matchmakingEvent);

        gameMatch.setCreatedByUser(creatingUser);
        gameMatch.setUpdatedByUser(creatingUser);

        gameMatchRepository.save(gameMatch);

        return gameMatch;
    }

    public GameMatch scheduleMatch(GameMatch match) {
        if(!match.getCompetition().isActive()) {
            throw new IllegalArgumentException("Competition is not active");
        }

        GameMatchJob job = GameMatchJob.from(match);
        try {
            rabbitMQService.enqueueGameMatchJob(job);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to enqueue game match job", ex);
        }

        match.setStatus(MatchStatus.waiting);
        match.setScheduledAt(Instant.now());

        return gameMatchRepository.save(match);
    }

    public Page<GameMatch> getPaginatedMatches(
            Competition competition,
            String ladderSlug,
            PageRequest page
    ) {
        Specification<GameMatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(competition != null) {
                predicates.add(cb.equal(root.get("competition"), competition));
            }

            if(ladderSlug != null) {
                predicates.add(cb.equal(root.get("ladder"), ladderSlug));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return gameMatchRepository.findAll(spec, page);
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

    public StatsDTO getTeamStatsByMatchReason(String teamUuid, MatchReason reason) {
        List<GameMatch> matches = gameMatchRepository.findTeamMatchesByReason(UUID.fromString(teamUuid), List.of(reason));
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
