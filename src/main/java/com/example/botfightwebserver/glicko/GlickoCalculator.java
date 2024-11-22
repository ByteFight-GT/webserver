package com.example.botfightwebserver.glicko;

import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.player.Player;
import java.util.Arrays;
import java.util.List;

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
        private Rating updatedPlayer1;
        private Rating updatedPlayer2;

        public MatchResult(double score1, double score2, Rating updatedPlayer1, Rating updatedPlayer2) {
            this.score1 = score1;
            this.score2 = score2;
            this.updatedPlayer1 = updatedPlayer1;
            this.updatedPlayer2 = updatedPlayer2;
        }

        public double getScore1() {
            return score1;
        }

        public double getScore2() {
            return score2;
        }

        public Rating getUpdatedPlayer1() {
            return updatedPlayer1;
        }

        public Rating getUpdatedPlayer2() {
            return updatedPlayer2;
        }

        // Convenience methods to access mu, phi, sigma
        public double getPlayer1Mu() {
            return updatedPlayer1.getMu();
        }

        public double getPlayer1Phi() {
            return updatedPlayer1.getPhi();
        }

        public double getPlayer1Sigma() {
            return updatedPlayer1.getSigma();
        }

        public double getPlayer2Mu() {
            return updatedPlayer2.getMu();
        }

        public double getPlayer2Phi() {
            return updatedPlayer2.getPhi();
        }

        public double getPlayer2Sigma() {
            return updatedPlayer2.getSigma();
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

    public Rating rate(Rating rating, List<Double> game) {
        rating = scaleDown(rating, RATIO);

        double varianceInv = 0;
        double difference = 0;

        double actualScore = game.get(0);
        Rating opponent = scaleDown(createRating(game.get(1), game.get(2), game.get(3)), RATIO);
        double impact = reduceImpact(opponent);
        double expectedScore = expectScore(rating, opponent, impact);
        varianceInv += Math.pow(impact, 2) * expectedScore * (1 - expectedScore);
        difference += impact * (actualScore - expectedScore);

        double variance = 1.0 / varianceInv;
        difference /= varianceInv;

        double sigma = determineSigma(rating, difference, variance);
        double phiStar = Math.sqrt(Math.pow(rating.getPhi(), 2) + Math.pow(sigma, 2));
        double phi = 1.0 / Math.sqrt(1.0 / Math.pow(phiStar, 2) + 1.0 / variance);
        double mu = rating.getMu() + Math.pow(phi, 2) * (difference / variance);

        return scaleUp(createRating(mu, phi, sigma), RATIO);
    }

    public MatchResult rate1vs1(Rating rating1, Rating rating2, String winner) {
        double score1;
        double score2;

        switch (winner.toLowerCase()) {
            case "player1":
                score1 = 1.0;
                score2 = 0.0;
                break;
            case "player2":
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

        Rating updatedPlayer1 = rate(rating1, Arrays.asList(score1, rating2.getMu(), rating2.getPhi(), rating2.getSigma()));
        Rating updatedPlayer2 = rate(rating2, Arrays.asList(score2, rating1.getMu(), rating1.getPhi(), rating1.getSigma()));

        return new MatchResult(score1, score2, updatedPlayer1, updatedPlayer2);
    }

    public double determineSigma(Rating rating, double difference, double variance) {
        // Implement the sigma calculation if needed
        return 0.0;
    }

    // Optional: Method to calculate Elo changes directly
    public GlickoChanges calculateGlicko(Player player1, Player player2, MATCH_STATUS matchStatus) {
        // Validate the match status
        if (matchStatus == MATCH_STATUS.IN_PROGRESS ||
                matchStatus == MATCH_STATUS.FAILED ||
                matchStatus == MATCH_STATUS.WAITING) {
            throw new IllegalArgumentException("Match must have a determined result. Match was in state: " + matchStatus);
        }

        // Validate player Elo ratings
        if (player1.getGlicko() == null || player2.getGlicko() == null) {
            throw new IllegalArgumentException("Player Elo cannot be null.");
        }

        // Create player ratings
        Rating player1Rating = new Rating(player1.getGlicko(), player1.getPhi(), player1.getSigma());
        Rating player2Rating = new Rating(player2.getGlicko(), player2.getPhi(), player2.getSigma());

        // Determine match status for Glicko calculation
        String glickoMatchStatus;
        if (matchStatus == MATCH_STATUS.DRAW) {
            glickoMatchStatus = "draw";
        } else if (matchStatus == MATCH_STATUS.PLAYER_ONE_WIN) {
            glickoMatchStatus = "player1";
        } else if (matchStatus == MATCH_STATUS.PLAYER_TWO_WIN) {
            glickoMatchStatus = "player2";
        } else {
            throw new IllegalArgumentException("Unknown match status: " + matchStatus);
        }

        MatchResult result = rate1vs1(player1Rating, player2Rating, glickoMatchStatus);

        return new GlickoChanges(
                result.getUpdatedPlayer1().getMu() - player1.getGlicko(),
                result.getUpdatedPlayer2().getMu() - player2.getGlicko(),
                result.getUpdatedPlayer1().getPhi() - player1.getPhi(),
                result.getUpdatedPlayer2().getPhi() - player2.getPhi(),
                result.getUpdatedPlayer1().getSigma() - player1.getSigma(),
                result.getUpdatedPlayer2().getSigma() - player2.getSigma()
        );
    }
}
