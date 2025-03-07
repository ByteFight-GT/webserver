package com.example.botfightwebserver.gameMatchLogs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameMatchLogRepository extends JpaRepository<GameMatchLog, Long> {
}

