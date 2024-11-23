package com.example.botfightwebserver.gameMatchLogs;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class GameMatchLogService {

    private final GameMatchLogRepository gameMatchLogRepository;

    public GameMatchLog createGameMatchLog(Long gameMatchId, String logs, double player1GlickoChange, double player2GlickoChange) {
        GameMatchLog gameMatchLog = new GameMatchLog();
        gameMatchLog.setMatchId(gameMatchId);
        gameMatchLog.setMatchLog(logs);
        gameMatchLog.setPlayer1GlickoChange(player1GlickoChange);
        gameMatchLog.setPlayer2GlickoChange(player2GlickoChange);
        return gameMatchLogRepository.save(gameMatchLog);
    }
}
