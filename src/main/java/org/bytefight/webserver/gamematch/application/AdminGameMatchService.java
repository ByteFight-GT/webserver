package org.bytefight.webserver.gamematch.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.gamematch.domain.dto.AdminGameMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminCreateMatchDto;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.domain.Ladder;
import org.bytefight.webserver.glicko.infra.LadderRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminGameMatchService {
    private final CompetitionRepository competitionRepository;
    private final GameMatchRepository gameMatchRepository;
    private final LadderRepository ladderRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final GameMatchService gameMatchService;

    public Page<AdminGameMatchDto> list(Specification<GameMatch> specs, Pageable pageable) {
        return gameMatchRepository.findAll(specs, pageable).map(AdminGameMatchDto::fromEntity);
    }

    public GameMatch createMatch(AdminCreateMatchDto input) {
        Competition competition = competitionRepository.findById(input.getCompetitionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
        Ladder ladder = ladderRepository.findById(input.getLadderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));
        if (!ladder.getCompetition().getId().equals(competition.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ladder does not belong to competition");
        }

        Team teamA = teamRepository.findById(input.getTeamAId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team A not found"));
        Team teamB = teamRepository.findById(input.getTeamBId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team B not found"));
        if (!teamA.getCompetition().getId().equals(competition.getId())
                || !teamB.getCompetition().getId().equals(competition.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teams do not belong to competition");
        }

        Submission submissionA = submissionRepository.findById(input.getTeamASubmissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team A submission not found"));
        Submission submissionB = submissionRepository.findById(input.getTeamBSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team B submission not found"));
        if (!submissionA.getTeam().getId().equals(teamA.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team A submission does not belong to Team A");
        }
        if (!submissionB.getTeam().getId().equals(teamB.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team B submission does not belong to Team B");
        }

        GameMatch match = gameMatchService.createMatch(
                null,
                teamA,
                teamB,
                submissionA,
                submissionB,
                ladder.getLadder(),
                input.getReason(),
                null
        );

        return gameMatchService.scheduleMatch(match);
    }
}
