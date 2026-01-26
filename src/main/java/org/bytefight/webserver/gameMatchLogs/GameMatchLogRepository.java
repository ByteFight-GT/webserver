package org.bytefight.webserver.gameMatchLogs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameMatchLogRepository extends JpaRepository<GameMatchLog, Long> {
    Optional<GameMatchLog> findByGameMatchUuid(UUID uuid);
}

