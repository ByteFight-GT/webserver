package org.bytefight.webserver.glicko.infra;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamGlickoHistoryRepository extends JpaRepository<TeamGlickoHistory, Long> {
    boolean existsByGameMatch(GameMatch gameMatch);
}
