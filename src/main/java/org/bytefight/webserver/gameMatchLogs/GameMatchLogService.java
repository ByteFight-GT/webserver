package org.bytefight.webserver.gameMatchLogs;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@AllArgsConstructor
public class GameMatchLogService {

    private final GameMatchLogRepository gameMatchLogRepository;

    public GameMatchLog createGameMatchLog(GameMatch gameMatch, String logs, double team1GlickoChange, double team2GlickoChange) {
        GameMatchLog gameMatchLog = new GameMatchLog();
        gameMatchLog.setGameMatch(gameMatch);
        gameMatchLog.setMatchLog(logs);
        gameMatchLog.setTeam1GlickoChange(team1GlickoChange);
        gameMatchLog.setTeam2GlickoChange(team2GlickoChange);
        return gameMatchLogRepository.save(gameMatchLog);
    }

    public List<GameMatchLog> getAllGameMatchLogs() {
        return gameMatchLogRepository.findAll();
    }

    public Optional<GameMatchLog> getGameMatchLogById(Long gameMatchLogId) {
        return gameMatchLogRepository.findById(gameMatchLogId);
    }

    public Optional<GameMatchLog> getGameMatchLog(String uuid) {
        return gameMatchLogRepository.findByGameMatchUuid(UUID.fromString(uuid));
    }

    public List<Long> getGameMatchLogIds() {
        return gameMatchLogRepository.findAll().stream().map(GameMatchLog::getId).toList();
    }

    public Optional<GameMatchLog> getMatchLogFromGame(Long gameMatchId) {
        return gameMatchLogRepository.findById(gameMatchId);
    }

    public Long getGameMatchLogCount() {
        return gameMatchLogRepository.count();
    }
}
