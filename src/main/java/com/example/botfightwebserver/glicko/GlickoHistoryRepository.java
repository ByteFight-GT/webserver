package com.example.botfightwebserver.glicko;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlickoHistoryRepository extends JpaRepository<GlickoHistory, Long> {
    List<GlickoHistory> findByTeamId(Long teamId);
}
