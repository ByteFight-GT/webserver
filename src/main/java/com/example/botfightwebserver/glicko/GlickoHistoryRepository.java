package com.example.botfightwebserver.glicko;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GlickoHistoryRepository extends JpaRepository<GlickoHistory, Long> {
    List<GlickoHistory> findByTeamId(Long teamId);
    List<GlickoHistory> findByTeamUuid(UUID uuid);
}
