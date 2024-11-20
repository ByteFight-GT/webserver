package com.example.botfightwebserver.elo;

import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EloCalculatorTest {

    @Test
    void calculateElo_Player1Wins_DefaultKFactor() {
        EloCalculator calculator = EloCalculator.builder().build();

        Player player1 = Player.builder().elo(1600.0).matchesPlayed(25).build();
        Player player2 = Player.builder().elo(1500.0).matchesPlayed(30).build();

        EloChanges changes = calculator.calculateElo(player1, player2, MATCH_STATUS.PLAYER_ONE_WIN);

        double expectedPlayer1Change = 20 * (1.0 - 1.0 / (1.0 + Math.pow(10, (1500.0 - 1600.0) / 400.0)));
        double expectedPlayer2Change = 20 * (0.0 - 1.0 / (1.0 + Math.pow(10, (1600.0 - 1500.0) / 400.0)));

        assertEquals(expectedPlayer1Change, changes.getPlayer1Change(), "Player 1's ELO change is incorrect.");
        assertEquals(expectedPlayer2Change, changes.getPlayer2Change(), "Player 2's ELO change is incorrect.");
    }

    @Test
    void calculateElo_Player2Wins_DefaultKFactor() {
        EloCalculator calculator = EloCalculator.builder().build();

        Player player1 = Player.builder().elo(1600.0).matchesPlayed(25).build();
        Player player2 = Player.builder().elo(1500.0).matchesPlayed(30).build();

        EloChanges changes = calculator.calculateElo(player1, player2, MATCH_STATUS.PLAYER_TWO_WIN);

        double expectedPlayer1Change = 20 * (0.0 - 1.0 / (1.0 + Math.pow(10, (1500.0 - 1600.0) / 400.0)));
        double expectedPlayer2Change = 20 * (1.0 - 1.0 / (1.0 + Math.pow(10, (1600.0 - 1500.0) / 400.0)));

        assertEquals(expectedPlayer1Change, changes.getPlayer1Change(), "Player 1's ELO change is incorrect.");
        assertEquals(expectedPlayer2Change, changes.getPlayer2Change(), "Player 2's ELO change is incorrect.");
    }

    @Test
    void calculateElo_Player1Wins_Expert_Vs_Newbie() {
        EloCalculator calculator = EloCalculator.builder().build();

        Player player1 = Player.builder().elo(2500.0).matchesPlayed(25).build();
        Player player2 = Player.builder().elo(1200.0).matchesPlayed(19).build();

        EloChanges changes = calculator.calculateElo(player1, player2, MATCH_STATUS.PLAYER_ONE_WIN);

        double expectedPlayer1Change = 10 * (1.0 - 1.0 / (1.0 + Math.pow(10, (1200.0 - 2500.0) / 400.0)));
        double expectedPlayer2Change = 40 * (0.0 - 1.0 / (1.0 + Math.pow(10, (2500.0 - 1200.0) / 400.0)));

        assertEquals(expectedPlayer1Change, changes.getPlayer1Change(), "Player 1's ELO change is incorrect.");
        assertEquals(expectedPlayer2Change, changes.getPlayer2Change(), "Player 2's ELO change is incorrect.");
    }

    @Test
    void calculateElo_Draw_DefaultKFactor() {
        EloCalculator calculator = EloCalculator.builder().build();

        Player player1 = Player.builder().elo(1600.0).matchesPlayed(25).build();
        Player player2 = Player.builder().elo(1500.0).matchesPlayed(30).build();

        EloChanges changes = calculator.calculateElo(player1, player2, MATCH_STATUS.DRAW);

        double player1Expected = 1.0 / (1.0 + Math.pow(10, (1500.0 - 1600.0) / 400.0));
        double player2Expected = 1.0 / (1.0 + Math.pow(10, (1600.0 - 1500.0) / 400.0));

        double expectedPlayer1Change = 20 * (0.5 - player1Expected);
        double expectedPlayer2Change = 20 * (0.5 - player2Expected);

        assertEquals(expectedPlayer1Change, changes.getPlayer1Change(), "Player 1's ELO change is incorrect.");
        assertEquals(expectedPlayer2Change, changes.getPlayer2Change(), "Player 2's ELO change is incorrect.");
    }

    @Test
    void calculateElo_InvalidMatchStatus() {
        EloCalculator calculator = EloCalculator.builder().build();

        Player player1 = Player.builder().elo(1600.0).matchesPlayed(25).build();
        Player player2 = Player.builder().elo(1500.0).matchesPlayed(30).build();

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateElo(player1, player2, MATCH_STATUS.IN_PROGRESS),
                "Invalid match status should throw IllegalArgumentException.");
    }
}
