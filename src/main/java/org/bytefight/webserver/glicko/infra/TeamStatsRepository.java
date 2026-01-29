package org.bytefight.webserver.glicko.infra;

import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats, Long> {
    Optional<TeamStats> findByTeamAndLadder(Team team, String ladder);
}
