package org.bytefight.webserver.glicko.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.glicko.domain.Ladder;
import org.bytefight.webserver.glicko.domain.dto.AdminCreateLadderDto;
import org.bytefight.webserver.glicko.domain.dto.AdminUpdateLadderDto;
import org.bytefight.webserver.glicko.infra.LadderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminLadderService {
    private static final double DEFAULT_GLICKO_DEFAULT_RATING = 1500.0;
    private static final double DEFAULT_GLICKO_DEFAULT_RD = 350.0;
    private static final double DEFAULT_GLICKO_RD_MAX = 350.0;
    private static final double DEFAULT_GLICKO_RD_MIN = 30.0;
    private static final double DEFAULT_GLICKO_PHI_INFLATION_PER_DAY = 0.0;
    private static final double DEFAULT_GLICKO_TAU = 0.5;
    private static final double DEFAULT_GLICKO_SIGMA_DEFAULT = 0.06;
    private static final double DEFAULT_GLICKO_SIGMA_MIN = 0.03;
    private static final double DEFAULT_GLICKO_SIGMA_MAX = 0.2;

    private final CompetitionRepository competitionRepository;
    private final LadderRepository ladderRepository;

    public AdminLadderService(CompetitionRepository competitionRepository, LadderRepository ladderRepository) {
        this.competitionRepository = competitionRepository;
        this.ladderRepository = ladderRepository;
    }

    public Page<Ladder> listByCompetitionId(Long competitionId, Pageable pageable) {
        return ladderRepository.findByCompetitionId(competitionId, pageable);
    }

    public Ladder createLadder(AdminCreateLadderDto input) {
        Competition competition = competitionRepository.findById(input.getCompetitionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
        ladderRepository.findByCompetitionAndLadder(competition, input.getLadder())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ladder already exists");
                });

        Ladder ladder = new Ladder();
        ladder.setCompetition(competition);
        ladder.setLadder(input.getLadder());
        ladder.setGlickoDefaultRating(valueOrDefault(input.getGlickoDefaultRating(), DEFAULT_GLICKO_DEFAULT_RATING));
        ladder.setGlickoDefaultRd(valueOrDefault(input.getGlickoDefaultRd(), DEFAULT_GLICKO_DEFAULT_RD));
        ladder.setGlickoRdMax(valueOrDefault(input.getGlickoRdMax(), DEFAULT_GLICKO_RD_MAX));
        ladder.setGlickoRdMin(valueOrDefault(input.getGlickoRdMin(), DEFAULT_GLICKO_RD_MIN));
        ladder.setGlickoPhiInflationPerDay(valueOrDefault(input.getGlickoPhiInflationPerDay(), DEFAULT_GLICKO_PHI_INFLATION_PER_DAY));
        ladder.setGlickoTau(valueOrDefault(input.getGlickoTau(), DEFAULT_GLICKO_TAU));
        ladder.setGlickoSigmaDefault(valueOrDefault(input.getGlickoSigmaDefault(), DEFAULT_GLICKO_SIGMA_DEFAULT));
        ladder.setGlickoSigmaMin(valueOrDefault(input.getGlickoSigmaMin(), DEFAULT_GLICKO_SIGMA_MIN));
        ladder.setGlickoSigmaMax(valueOrDefault(input.getGlickoSigmaMax(), DEFAULT_GLICKO_SIGMA_MAX));

        return ladderRepository.save(ladder);
    }

    public Ladder updateLadder(Long id, AdminUpdateLadderDto input) {
        Ladder ladder = ladderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));

        if (input.getGlickoDefaultRating() != null) {
            ladder.setGlickoDefaultRating(input.getGlickoDefaultRating());
        }
        if (input.getGlickoDefaultRd() != null) {
            ladder.setGlickoDefaultRd(input.getGlickoDefaultRd());
        }
        if (input.getGlickoRdMax() != null) {
            ladder.setGlickoRdMax(input.getGlickoRdMax());
        }
        if (input.getGlickoRdMin() != null) {
            ladder.setGlickoRdMin(input.getGlickoRdMin());
        }
        if (input.getGlickoPhiInflationPerDay() != null) {
            ladder.setGlickoPhiInflationPerDay(input.getGlickoPhiInflationPerDay());
        }
        if (input.getGlickoTau() != null) {
            ladder.setGlickoTau(input.getGlickoTau());
        }
        if (input.getGlickoSigmaDefault() != null) {
            ladder.setGlickoSigmaDefault(input.getGlickoSigmaDefault());
        }
        if (input.getGlickoSigmaMin() != null) {
            ladder.setGlickoSigmaMin(input.getGlickoSigmaMin());
        }
        if (input.getGlickoSigmaMax() != null) {
            ladder.setGlickoSigmaMax(input.getGlickoSigmaMax());
        }

        return ladderRepository.save(ladder);
    }

    public Ladder getLadder(Long id) {
        return ladderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));
    }

    private static double valueOrDefault(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
