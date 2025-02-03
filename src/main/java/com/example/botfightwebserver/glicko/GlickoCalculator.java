package com.example.botfightwebserver.glicko;

import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.team.Team;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class GlickoCalculator {

    public static final double MU = 1500.0;
    public static final double PHI = 350.0;
    public static final double SIGMA = 0.06;
    public static final double TAU = 1.0;
    public static final double EPSILON = 0.000001;
    public static final double RATIO = 173.7178;

    public class Rating {
        private double mu;
        private double phi;
        private double sigma;

        public Rating(double mu, double phi, double sigma) {
            this.mu = mu;
            this.phi = phi;
            this.sigma = sigma;
        }

        public double getMu() {
            return mu;
        }

        public double getPhi() {
            return phi;
        }

        public double getSigma() {
            return sigma;
        }

        @Override
        public String toString() {
            return String.format("Rating(mu=%.3f, phi=%.3f, sigma=%.3f)", mu, phi, sigma);
        }
    }

    public class MatchResult {
        private double score1;
        private double score2;
        private Rating updatedTeam1;
        private Rating updatedTeam2;

        public MatchResult(double score1, double score2, Rating updatedTeam1, Rating updatedTeam2) {
            this.score1 = score1;
            this.score2 = score2;
            this.updatedTeam1 = updatedTeam1;
            this.updatedTeam2 = updatedTeam2;
        }

        public double getScore1() {
            return score1;
        }

        public double getScore2() {
            return score2;
        }

        public Rating getUpdatedTeam1() {
            return updatedTeam1;
        }

        public Rating getUpdatedTeam2() {
            return updatedTeam2;
        }

        // Convenience methods to access mu, phi, sigma
        public double getTeam1Mu() {
            return updatedTeam1.getMu();
        }

        public double getTeam1Phi() {
            return updatedTeam1.getPhi();
        }

        public double getTeam1Sigma() {
            return updatedTeam1.getSigma();
        }

        public double getTeam2Mu() {
            return updatedTeam2.getMu();
        }

        public double getTeam2Phi() {
            return updatedTeam2.getPhi();
        }

        public double getTeam2Sigma() {
            return updatedTeam2.getSigma();
        }
    }

    public Rating createRating(Double mu, Double phi, Double sigma) {
        if (mu == null) mu = MU;
        if (phi == null) phi = PHI;
        if (sigma == null) sigma = SIGMA;
        return new Rating(mu, phi, sigma);
    }

    public Rating scaleDown(Rating rating, double ratio) {
        double mu = (rating.getMu() - MU) / ratio;
        double phi = rating.getPhi() / ratio;
        return createRating(mu, phi, rating.getSigma());
    }

    public Rating scaleUp(Rating rating, double ratio) {
        double mu = rating.getMu() * ratio + MU;
        double phi = rating.getPhi() * ratio;
        return createRating(mu, phi, rating.getSigma());
    }

    public double reduceImpact(Rating rating) {
        return 1.0 / Math.sqrt(1.0 + (3 * Math.pow(rating.getPhi(), 2)) / Math.pow(Math.PI, 2));
    }

    public double expectScore(Rating rating, Rating otherRating, double impact) {
        return 1.0 / (1.0 + Math.exp(-impact * (rating.getMu() - otherRating.getMu())));
    }

    // public Rating rate(Rating rating, List<Double> game) {
    //     rating = scaleDown(rating, RATIO);

    //     double varianceInv = 0;
    //     double difference = 0;

    //     double actualScore = game.get(0);
    //     Rating opponent = scaleDown(createRating(game.get(1), game.get(2), game.get(3)), RATIO);
    //     double impact = reduceImpact(opponent);
    //     double expectedScore = expectScore(rating, opponent, impact);
    //     varianceInv += Math.pow(impact, 2) * expectedScore * (1 - expectedScore);
    //     difference += impact * (actualScore - expectedScore);

    //     double variance = 1.0 / varianceInv;
    //     difference /= varianceInv;

    //     double sigma = determineSigma(rating, difference, variance);
    //     double phiStar = Math.sqrt(Math.pow(rating.getPhi(), 2) + Math.pow(sigma, 2));
    //     double phi = 1.0 / Math.sqrt(1.0 / Math.pow(phiStar, 2) + 1.0 / variance);
    //     double mu = rating.getMu() + Math.pow(phi, 2) * (difference / variance);

    //     return scaleUp(createRating(mu, phi, sigma), RATIO);
    // }

    public Rating rate(Rating rating, List<Double> game) {
        double K = 32

        double rating1 = rating.getMu()
        double rating2 = game.get(1)
        double score = game.get(0)

        double expectedScore = 1.0 / (1.0 + Math.pow(10, (rating2 - rating1) / 400));
        double newRating = rating1 + K * (score - expectedScore);

        return new Rating(newRating, rating.getPhi(), rating.getSigma());
    }

    public MatchResult rate1vs1(Rating rating1, Rating rating2, String winner) {
        double score1;
        double score2;

        switch (winner.toLowerCase()) {
            case "team1":
                score1 = 1.0;
                score2 = 0.0;
                break;
            case "team2":
                score1 = 0.0;
                score2 = 1.0;
                break;
            case "draw":
                score1 = 0.5;
                score2 = 0.5;
                break;
            default:
                throw new IllegalArgumentException("Invalid winner: " + winner);
        }

        Rating updatedTeam1 = rate(rating1, Arrays.asList(score1, rating2.getMu(), rating2.getPhi(), rating2.getSigma()));
        Rating updatedTeam2 = rate(rating2, Arrays.asList(score2, rating1.getMu(), rating1.getPhi(), rating1.getSigma()));

        return new MatchResult(score1, score2, updatedTeam1, updatedTeam2);
    }

    public double determineSigma(Rating rating, double difference, double variance) {
        // Implement the sigma calculation if needed
        return 0.0;
    }

    // Optional: Method to calculate Elo changes directly
    public GlickoChanges calculateGlicko(Team team1, Team team2, MATCH_STATUS matchStatus) {
        // Validate the match status
        if (matchStatus == MATCH_STATUS.IN_PROGRESS ||
                matchStatus == MATCH_STATUS.FAILED ||
                matchStatus == MATCH_STATUS.WAITING) {
            log.error("Match must have a determined result. Match was in state: " + matchStatus);
        }

        // Validate team Elo ratings
        if (team1.getGlicko() == null || team2.getGlicko() == null) {
            log.error("Team Elo cannot be null.");
        }

        // Create team ratings
        Rating team1Rating = new Rating(team1.getGlicko(), team1.getPhi(), team1.getSigma());
        Rating team2Rating = new Rating(team2.getGlicko(), team2.getPhi(), team2.getSigma());

        // Determine match status for Glicko calculation
        String glickoMatchStatus;
        if (matchStatus == MATCH_STATUS.DRAW) {
            glickoMatchStatus = "draw";
        } else if (matchStatus == MATCH_STATUS.TEAM_ONE_WIN) {
            glickoMatchStatus = "team1";
        } else if (matchStatus == MATCH_STATUS.TEAM_TWO_WIN) {
            glickoMatchStatus = "team2";
        } else {
            log.error("Team Elo cannot be null.");
            return new GlickoChanges();
        }

        MatchResult result = rate1vs1(team1Rating, team2Rating, glickoMatchStatus);

        return new GlickoChanges(
                result.getUpdatedTeam1().getMu() - team1.getGlicko(),
                result.getUpdatedTeam2().getMu() - team2.getGlicko(),
                result.getUpdatedTeam1().getPhi() - team1.getPhi(),
                result.getUpdatedTeam2().getPhi() - team2.getPhi(),
                result.getUpdatedTeam1().getSigma() - team1.getSigma(),
                result.getUpdatedTeam2().getSigma() - team2.getSigma()
        );
    }
}
