package com.example.botfightwebserver.glicko;

import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.team.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlickoCalculatorTest {

    @Test
    void testCreateRatingWithNullValues() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating = calculator.createRating(null, null, null);

        assertNotNull(rating, "Rating should not be null");
        assertEquals(GlickoCalculator.MU, rating.getMu(), "Default mu should be MU constant");
        assertEquals(GlickoCalculator.PHI, rating.getPhi(), "Default phi should be PHI constant");
        assertEquals(GlickoCalculator.SIGMA, rating.getSigma(), "Default sigma should be SIGMA constant");
    }

    @Test
    void testCreateRatingWithValues() {
        GlickoCalculator calculator = new GlickoCalculator();

        double mu = 1600.0;
        double phi = 200.0;
        double sigma = 0.05;
        GlickoCalculator.Rating rating = calculator.createRating(mu, phi, sigma);

        assertNotNull(rating, "Rating should not be null");
        assertEquals(mu, rating.getMu(), "Mu should match the provided value");
        assertEquals(phi, rating.getPhi(), "Phi should match the provided value");
        assertEquals(sigma, rating.getSigma(), "Sigma should match the provided value");
    }

    @Test
    void testScaleDown() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating = calculator.createRating(1600.0, 200.0, 0.06);
        GlickoCalculator.Rating scaledDownRating = calculator.scaleDown(rating, GlickoCalculator.RATIO);

        assertNotNull(scaledDownRating, "Scaled down rating should not be null");
        double expectedMu = (1600.0 - GlickoCalculator.MU) / GlickoCalculator.RATIO;
        double expectedPhi = 200.0 / GlickoCalculator.RATIO;

        assertEquals(expectedMu, scaledDownRating.getMu(), 0.0001, "Scaled mu should be correct");
        assertEquals(expectedPhi, scaledDownRating.getPhi(), 0.0001, "Scaled phi should be correct");
        assertEquals(0.06, scaledDownRating.getSigma(), "Sigma should remain unchanged");
    }

    @Test
    void testScaleUp() {
        GlickoCalculator calculator = new GlickoCalculator();

        double mu = 0.5;
        double phi = 1.2;
        GlickoCalculator.Rating rating = calculator.createRating(mu, phi, 0.06);
        GlickoCalculator.Rating scaledUpRating = calculator.scaleUp(rating, GlickoCalculator.RATIO);

        assertNotNull(scaledUpRating, "Scaled up rating should not be null");
        double expectedMu = mu * GlickoCalculator.RATIO + GlickoCalculator.MU;
        double expectedPhi = phi * GlickoCalculator.RATIO;

        assertEquals(expectedMu, scaledUpRating.getMu(), 0.0001, "Scaled mu should be correct");
        assertEquals(expectedPhi, scaledUpRating.getPhi(), 0.0001, "Scaled phi should be correct");
        assertEquals(0.06, scaledUpRating.getSigma(), "Sigma should remain unchanged");
    }

    @Test
    void testReduceImpact() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating = calculator.createRating(0.0, 1.0, 0.06);
        double impact = calculator.reduceImpact(rating);

        assertTrue(impact > 0 && impact <= 1.0, "Impact should be between 0 and 1");
    }

    @Test
    void testExpectScoreEqualRatings() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating1 = calculator.createRating(0.0, 1.0, 0.06);
        GlickoCalculator.Rating rating2 = calculator.createRating(0.0, 1.0, 0.06);
        double impact = calculator.reduceImpact(rating2);

        double expectedScore = calculator.expectScore(rating1, rating2, impact);

        assertEquals(0.5, expectedScore, 0.0001, "Expected score should be 0.5 for equal ratings");
    }

    @Test
    void testExpectScoreDifferentRatings() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating1 = calculator.createRating(1.0, 1.0, 0.06);
        GlickoCalculator.Rating rating2 = calculator.createRating(-1.0, 1.0, 0.06);
        double impact = calculator.reduceImpact(rating2);

        double expectedScore = calculator.expectScore(rating1, rating2, impact);

        assertTrue(expectedScore > 0.5, "Expected score should be greater than 0.5 when rating1 > rating2");
    }

    @Test
    void testRate1vs1Team1Wins() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating1 = calculator.createRating(1500.0, 200.0, 0.06);
        GlickoCalculator.Rating rating2 = calculator.createRating(1400.0, 30.0, 0.06);

        GlickoCalculator.MatchResult result = calculator.rate1vs1(rating1, rating2, "team1");

        assertNotNull(result, "Match result should not be null");
        assertEquals(1.0, result.getScore1(), "Team 1 score should be 1.0");
        assertEquals(0.0, result.getScore2(), "Team 2 score should be 0.0");

        assertNotNull(result.getUpdatedTeam1(), "Updated Team 1 rating should not be null");
        assertNotNull(result.getUpdatedTeam2(), "Updated Team 2 rating should not be null");
    }

    @Test
    void testRate1vs1Draw() {
        GlickoCalculator calculator = new GlickoCalculator();

        GlickoCalculator.Rating rating1 = calculator.createRating(1500.0, 200.0, 0.06);
        GlickoCalculator.Rating rating2 = calculator.createRating(1500.0, 200.0, 0.06);

        GlickoCalculator.MatchResult result = calculator.rate1vs1(rating1, rating2, "draw");

        assertNotNull(result, "Match result should not be null");
        assertEquals(0.5, result.getScore1(), "Team 1 score should be 0.5");
        assertEquals(0.5, result.getScore2(), "Team 2 score should be 0.5");

        assertNotNull(result.getUpdatedTeam1(), "Updated Team 1 rating should not be null");
        assertNotNull(result.getUpdatedTeam2(), "Updated Team 2 rating should not be null");
    }

    @Test
    void testCalculateGlickoTeamOneWins() {
        GlickoCalculator calculator = new GlickoCalculator();

        Team team1 = Team.builder()
                .name("Team One")
                .glicko(1500.0)
                .phi(200.0)
                .sigma(0.06)
                .build();

        Team team2 = Team.builder()
                .name("Team Two")
                .glicko(1400.0)
                .phi(30.0)
                .sigma(0.06)
                .build();

        GlickoChanges changes = calculator.calculateGlicko(team1, team2, MATCH_STATUS.TEAM_ONE_WIN);

        assertNotNull(changes, "GlickoChanges should not be null");
        assertTrue(changes.getTeam1Change() > 0, "Team 1 Elo change should be positive");
        assertTrue(changes.getTeam2Change() < 0, "Team 2 Elo change should be negative");
    }

    @Test
    void testCalculateGlickoDraw() {
        GlickoCalculator calculator = new GlickoCalculator();

        Team team1 = Team.builder()
                .name("Team One")
                .glicko(1500.0)
                .phi(200.0)
                .sigma(0.06)
                .build();

        Team team2 = Team.builder()
                .name("Team Two")
                .glicko(1500.0)
                .phi(200.0)
                .sigma(0.06)
                .build();

        GlickoChanges changes = calculator.calculateGlicko(team1, team2, MATCH_STATUS.DRAW);

        assertNotNull(changes, "GlickoChanges should not be null");
        assertEquals(0.0, changes.getTeam1Change(), 0.0001, "Team 1 Elo change should be zero");
        assertEquals(0.0, changes.getTeam2Change(), 0.0001, "Team 2 Elo change should be zero");
    }

    @Test
    void testCalculateGlickoInvalidMatchStatus() {
        GlickoCalculator calculator = new GlickoCalculator();

        Team team1 = Team.builder()
                .name("Team One")
                .glicko(1500.0)
                .phi(200.0)
                .sigma(0.06)
                .build();

        Team team2 = Team.builder()
                .name("Team Two")
                .glicko(1500.0)
                .phi(200.0)
                .sigma(0.06)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateGlicko(team1, team2, MATCH_STATUS.IN_PROGRESS);
        }, "Should throw IllegalArgumentException for invalid match status");
    }
}
