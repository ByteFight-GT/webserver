package com.example.botfightwebserver.elo;

import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerDTO;
import org.springframework.stereotype.Service;

@Service
public class EloCalculator {

    private static final int K_FACTOR_DEFAULT = 20;
    private static final int K_FACTOR_NEW_PLAYER = 40;
    private static final int K_FACTOR_HIGH_RATED = 10;
    private static final int NEW_PLAYER_THRESHOLD = 20;

    public EloChanges calculateElo(Player player1, Player player2, MATCH_STATUS matchStatus) {
        if (matchStatus == MATCH_STATUS.IN_PROGRESS || matchStatus == MATCH_STATUS.FAILED || matchStatus == MATCH_STATUS.WAITING) {
            throw new IllegalArgumentException("Match must have determined result. Match was in state: " + matchStatus);
        }
        double player1Expected = calculateExpectedScore(player1.getElo(), player2.getElo());
        double player2Expected = calculateExpectedScore(player2.getElo(), player1.getElo());

        double player1Actual = 0;
        double player2Actual = 0;
        if (matchStatus == MATCH_STATUS.DRAW) {
            player1Actual = 0.5;
            player2Actual = 0.5;
        } else {
            player1Actual = matchStatus == MATCH_STATUS.PLAYER_ONE_WIN ? 1.0 : 0.0;
            player2Actual = matchStatus == MATCH_STATUS.PLAYER_TWO_WIN ? 1.0 : 0.0;
        }

        int player1KFactor = determineKFactor(player1.getElo(), player1.getMatchesPlayed());
        int player2KFactor = determineKFactor(player2.getElo(), player2.getMatchesPlayed());

        double player1Change = player1KFactor * (player1Actual - player1Expected);
        double player2Change = player2KFactor * (player2Actual - player2Expected);

        return new EloChanges(player1Change, player2Change);
    }

    private double calculateExpectedScore(double player1Elo, double player2Elo) {
        return 1.0 / (1.0 + Math.pow(10, (player2Elo - player1Elo) / 400.0));
    }

    private int determineKFactor(double playerElo, int gamesPlayed) {
        if (gamesPlayed < NEW_PLAYER_THRESHOLD) {
            return  K_FACTOR_NEW_PLAYER;
        }
        if (playerElo > 2400) {
            return K_FACTOR_HIGH_RATED;
        }
        return K_FACTOR_DEFAULT;
    }
}
