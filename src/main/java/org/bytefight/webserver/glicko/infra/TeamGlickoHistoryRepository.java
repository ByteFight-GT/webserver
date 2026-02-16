package org.bytefight.webserver.glicko.infra;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamGlickoHistoryRepository extends JpaRepository<TeamGlickoHistory, Long> {
    boolean existsByGameMatch(GameMatch gameMatch);

    @Query("SELECT tgh FROM TeamGlickoHistory tgh WHERE tgh.team = :team AND tgh.ladder = :ladder ORDER BY tgh.createdAt ASC")
    List<TeamGlickoHistory> findAllByTeamAndLadderOrderByCreatedAtAsc(Team team, String ladder);
}
