package org.bytefight.webserver.glicko.application;

import jakarta.transaction.Transactional;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.glicko.domain.GlickoMatchUpdate;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.glicko.infra.TeamGlickoHistoryRepository;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlickoService {
    private final LadderRepository ladderRepository;
    private final TeamGlickoHistoryRepository teamGlickoHistoryRepository;
    private final TeamStatsRepository teamStatsRepository;
    private final TeamStatsService teamStatsService;

    private static final double SCALE = 173.7178;
    private static final double EPSILON = 0.000001;

    public GlickoService(
            LadderRepository ladderRepository,
            TeamGlickoHistoryRepository teamGlickoHistoryRepository,
            TeamStatsRepository teamStatsRepository,
            TeamStatsService teamStatsService
    ) {
        this.ladderRepository = ladderRepository;
        this.teamGlickoHistoryRepository = teamGlickoHistoryRepository;
        this.teamStatsRepository = teamStatsRepository;
        this.teamStatsService = teamStatsService;
    }

    public List<TeamGlickoHistory> getTeamGlickoHistoryByTeamAndLadder(Team team, String ladder) {
        return teamGlickoHistoryRepository.findAllByTeamAndLadderOrderByCreatedAtAsc(team, ladder);
    }

    @Transactional
    public void processGameMatchResult(GameMatch gameMatch, boolean recalculate) {
        // Glicko updates should be idempotent or else TeamStats could diverge
        if (teamGlickoHistoryRepository.existsByGameMatch(gameMatch) && !recalculate) {
            return;
        }

        if (gameMatch.getStatus() != MatchStatus.draw && gameMatch.getStatus() != MatchStatus.team_a_win && gameMatch.getStatus() != MatchStatus.team_b_win && !recalculate) {
            throw new IllegalStateException("Game match result is not draw or team_a_win or team_b_win");
        }

        Team teamA = gameMatch.getTeamA();
        Team teamB = gameMatch.getTeamB();

        Ladder ladder = ladderRepository.findByCompetitionAndLadder(gameMatch.getCompetition(), gameMatch.getLadder())
                .orElseThrow(() -> new RuntimeException("Ladder " + gameMatch.getLadder() + " not found in competition " + gameMatch.getCompetition().getSlug()));

        TeamStats teamAStats = teamStatsService.getTeamStatsCreateIfNotExist(teamA, gameMatch.getLadder());
        TeamStats teamBStats = teamStatsService.getTeamStatsCreateIfNotExist(teamB, gameMatch.getLadder());

        GlickoMatchUpdate glickoMatchUpdate = calculateMatchUpdate(teamAStats, teamBStats, ladder, gameMatch.getStatus());

        applyGlickoMatchUpdate(glickoMatchUpdate, teamAStats, teamBStats);
    }

    public void applyGlickoMatchUpdate(GlickoMatchUpdate glickoMatchUpdate, TeamStats teamAStats, TeamStats teamBStats) {
        GlickoMatchUpdate.TeamUpdate teamAUpdate = glickoMatchUpdate.getTeamA();
        GlickoMatchUpdate.TeamUpdate teamBUpdate = glickoMatchUpdate.getTeamB();

        // Insert the Glicko history for both teams
        TeamGlickoHistory teamAGlickoHistory = new TeamGlickoHistory();
        teamAGlickoHistory.setTeam(teamAUpdate.getTeam());
        teamAGlickoHistory.setCompetition(glickoMatchUpdate.getCompetition());
        teamAGlickoHistory.setLadder(glickoMatchUpdate.getLadder().getLadder());
        teamAGlickoHistory.setOldGlicko(teamAUpdate.getOldRating());
        teamAGlickoHistory.setNewGlicko(teamAUpdate.getNewRating());

        TeamGlickoHistory teamBGlickoHistory = new TeamGlickoHistory();
        teamBGlickoHistory.setTeam(teamBUpdate.getTeam());
        teamBGlickoHistory.setCompetition(glickoMatchUpdate.getCompetition());
        teamBGlickoHistory.setLadder(glickoMatchUpdate.getLadder().getLadder());
        teamBGlickoHistory.setOldGlicko(teamBUpdate.getOldRating());
        teamBGlickoHistory.setNewGlicko(teamBUpdate.getNewRating());

        teamGlickoHistoryRepository.save(teamAGlickoHistory);
        teamGlickoHistoryRepository.save(teamBGlickoHistory);

        // Update each teams' TeamStats
        teamAStats.setGlickoRating(teamAUpdate.getNewRating());
        teamAStats.setGlickoRd(teamAUpdate.getNewRd());
        teamAStats.setGlickoVolatility(teamAUpdate.getNewVolatility());
        teamAStats.setMatchesPlayed(teamAStats.getMatchesPlayed() + teamAUpdate.getMatchesPlayedDelta());
        teamAStats.setWins(teamAStats.getWins() + teamAUpdate.getWinsDelta());
        teamAStats.setLosses(teamAStats.getLosses() + teamAUpdate.getLossesDelta());
        teamAStats.setDraws(teamAStats.getDraws() + teamAUpdate.getDrawsDelta());

        teamBStats.setGlickoRating(teamBUpdate.getNewRating());
        teamBStats.setGlickoRd(teamBUpdate.getNewRd());
        teamBStats.setGlickoVolatility(teamBUpdate.getNewVolatility());
        teamBStats.setMatchesPlayed(teamBStats.getMatchesPlayed() + teamBUpdate.getMatchesPlayedDelta());
        teamBStats.setWins(teamBStats.getWins() + teamBUpdate.getWinsDelta());
        teamBStats.setLosses(teamBStats.getLosses() + teamBUpdate.getLossesDelta());
        teamBStats.setDraws(teamBStats.getDraws() + teamBUpdate.getDrawsDelta());

        teamStatsRepository.save(teamAStats);
        teamStatsRepository.save(teamBStats);
    }

    public GlickoMatchUpdate calculateMatchUpdate(TeamStats teamAStats, TeamStats teamBStats, Ladder ladder, MatchStatus matchStatus) {
        double scoreA;
        double scoreB;

        if (matchStatus == MatchStatus.team_a_win) {
            scoreA = 1.0;
            scoreB = 0.0;
        } else if (matchStatus == MatchStatus.team_b_win) {
            scoreA = 0.0;
            scoreB = 1.0;
        } else if (matchStatus == MatchStatus.draw) {
            scoreA = 0.5;
            scoreB = 0.5;
        } else {
            throw new IllegalArgumentException("Match status must be team_a_win, team_b_win, or draw");
        }

        TeamUpdateResult updatedA = calculateUpdatedRating(
                teamAStats.getGlickoRating(),
                teamAStats.getGlickoRd(),
                teamAStats.getGlickoVolatility(),
                teamBStats.getGlickoRating(),
                teamBStats.getGlickoRd(),
                ladder,
                scoreA,
                ladder.getGlickoDefaultRating()
        );

        TeamUpdateResult updatedB = calculateUpdatedRating(
                teamBStats.getGlickoRating(),
                teamBStats.getGlickoRd(),
                teamBStats.getGlickoVolatility(),
                teamAStats.getGlickoRating(),
                teamAStats.getGlickoRd(),
                ladder,
                scoreB,
                ladder.getGlickoDefaultRating()
        );

        int winsDeltaA = matchStatus == MatchStatus.team_a_win ? 1 : 0;
        int lossesDeltaA = matchStatus == MatchStatus.team_b_win ? 1 : 0;
        int drawsDeltaA = matchStatus == MatchStatus.draw ? 1 : 0;

        int winsDeltaB = matchStatus == MatchStatus.team_b_win ? 1 : 0;
        int lossesDeltaB = matchStatus == MatchStatus.team_a_win ? 1 : 0;
        int drawsDeltaB = matchStatus == MatchStatus.draw ? 1 : 0;

        GlickoMatchUpdate.TeamUpdate teamAUpdate = new GlickoMatchUpdate.TeamUpdate();
        teamAUpdate.setTeam(teamAStats.getTeam());
        teamAUpdate.setOldRating(teamAStats.getGlickoRating());
        teamAUpdate.setOldRd(teamAStats.getGlickoRd());
        teamAUpdate.setOldVolatility(teamAStats.getGlickoVolatility());
        teamAUpdate.setNewRating(updatedA.newRating);
        teamAUpdate.setNewRd(updatedA.newRd);
        teamAUpdate.setNewVolatility(updatedA.newVolatility);
        teamAUpdate.setMatchesPlayedDelta(1);
        teamAUpdate.setWinsDelta(winsDeltaA);
        teamAUpdate.setLossesDelta(lossesDeltaA);
        teamAUpdate.setDrawsDelta(drawsDeltaA);

        GlickoMatchUpdate.TeamUpdate teamBUpdate = new GlickoMatchUpdate.TeamUpdate();
        teamBUpdate.setTeam(teamBStats.getTeam());
        teamBUpdate.setOldRating(teamBStats.getGlickoRating());
        teamBUpdate.setOldRd(teamBStats.getGlickoRd());
        teamBUpdate.setOldVolatility(teamBStats.getGlickoVolatility());
        teamBUpdate.setNewRating(updatedB.newRating);
        teamBUpdate.setNewRd(updatedB.newRd);
        teamBUpdate.setNewVolatility(updatedB.newVolatility);
        teamBUpdate.setMatchesPlayedDelta(1);
        teamBUpdate.setWinsDelta(winsDeltaB);
        teamBUpdate.setLossesDelta(lossesDeltaB);
        teamBUpdate.setDrawsDelta(drawsDeltaB);

        return new GlickoMatchUpdate(ladder.getCompetition(), ladder, teamAUpdate, teamBUpdate);
    }

    private TeamUpdateResult calculateUpdatedRating(
            double rating,
            double rd,
            double volatility,
            double opponentRating,
            double opponentRd,
            Ladder ladder,
            double score,
            double baseRating
    ) {
        double mu = (rating - baseRating) / SCALE;
        double phi = rd / SCALE;
        double muOpp = (opponentRating - baseRating) / SCALE;
        double phiOpp = opponentRd / SCALE;

        double g = 1.0 / Math.sqrt(1.0 + (3.0 * phiOpp * phiOpp) / (Math.PI * Math.PI));
        double e = 1.0 / (1.0 + Math.exp(-g * (mu - muOpp)));
        double v = 1.0 / (g * g * e * (1.0 - e));
        double delta = v * g * (score - e);

        double newSigma = solveSigma(phi, volatility, delta, v, ladder.getGlickoTau());
        double phiStar = Math.sqrt(phi * phi + newSigma * newSigma);
        double newPhi = 1.0 / Math.sqrt((1.0 / (phiStar * phiStar)) + (1.0 / v));
        double newMu = mu + (newPhi * newPhi) * g * (score - e);

        double newRating = newMu * SCALE + baseRating;
        double newRd = newPhi * SCALE;

        if (ladder.getGlickoRdMax() > 0) {
            newRd = Math.min(newRd, ladder.getGlickoRdMax());
        }
        if (ladder.getGlickoRdMin() != null) {
            newRd = Math.max(newRd, ladder.getGlickoRdMin());
        }

        if (ladder.getGlickoSigmaMin() != null) {
            newSigma = Math.max(newSigma, ladder.getGlickoSigmaMin());
        }
        if (ladder.getGlickoSigmaMax() != null) {
            newSigma = Math.min(newSigma, ladder.getGlickoSigmaMax());
        }

        return new TeamUpdateResult(newRating, newRd, newSigma);
    }

    private double solveSigma(double phi, double sigma, double delta, double v, double tau) {
        double a = Math.log(sigma * sigma);
        double aLower;
        double aUpper;

        double deltaSquared = delta * delta;
        double phiSquared = phi * phi;

        if (deltaSquared > phiSquared + v) {
            aLower = a;
            aUpper = Math.log(deltaSquared - phiSquared - v);
        } else {
            aLower = a;
            int k = 1;
            aUpper = a - (k * tau);
            while (f(aUpper, delta, phi, v, a, tau) < 0) {
                k++;
                aUpper = a - (k * tau);
            }
        }

        double fLower = f(aLower, delta, phi, v, a, tau);
        double fUpper = f(aUpper, delta, phi, v, a, tau);

        while (Math.abs(aUpper - aLower) > EPSILON) {
            double aMid = aLower + (aUpper - aLower) * fLower / (fLower - fUpper);
            double fMid = f(aMid, delta, phi, v, a, tau);

            if (fMid * fUpper < 0) {
                aLower = aUpper;
                fLower = fUpper;
            } else {
                fLower = fLower / 2.0;
            }

            aUpper = aMid;
            fUpper = fMid;
        }

        return Math.exp(aUpper / 2.0);
    }

    private double f(double x, double delta, double phi, double v, double a, double tau) {
        double ex = Math.exp(x);
        double num = ex * (delta * delta - phi * phi - v - ex);
        double den = 2.0 * Math.pow(phi * phi + v + ex, 2);
        return (num / den) - ((x - a) / (tau * tau));
    }

    private static class TeamUpdateResult {
        private final double newRating;
        private final double newRd;
        private final double newVolatility;

        private TeamUpdateResult(double newRating, double newRd, double newVolatility) {
            this.newRating = newRating;
            this.newRd = newRd;
            this.newVolatility = newVolatility;
        }
    }
}
