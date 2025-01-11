package com.example.botfightwebserver.gameMatchLogs;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class GameMatchLogService {

    private final GameMatchLogRepository gameMatchLogRepository;

    public GameMatchLog createGameMatchLog(Long gameMatchId, String logs, double team1GlickoChange, double team2GlickoChange) {
        GameMatchLog gameMatchLog = new GameMatchLog();
        gameMatchLog.setMatchId(gameMatchId);
        gameMatchLog.setMatchLog(logs);
        gameMatchLog.setTeam1GlickoChange(team1GlickoChange);
        gameMatchLog.setTeam2GlickoChange(team2GlickoChange);
        return gameMatchLogRepository.save(gameMatchLog);
    }

    public List<GameMatchLog> getAllGameMatchLogs() {
        return gameMatchLogRepository.findAll();
    }
}
