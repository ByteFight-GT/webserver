package org.bytefight.webserver.glicko.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.leaderboard.domain.LeaderboardRow;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats, Long> {
    Optional<TeamStats> findByTeamAndLadder(Team team, String ladder);
    List<TeamStats> findAllByCompetitionAndLadderAndTeamIn(Competition competition, String ladder, Collection<Team> teams);
    List<TeamStats> findAllByCompetitionAndLadderOrderByGlickoRatingDesc(Competition competition, String ladder);

    @Query(value = """
        SELECT
            t.uuid::text AS teamUuid,
            t.name AS teamName,
            t.quote AS teamQuote,
            t.type::text AS teamType,
            ts.glicko_rating AS glickoRating,
            CASE
                WHEN ts.matches_played = 0 THEN NULL
                ELSE DENSE_RANK() OVER (
                    ORDER BY (CASE WHEN ts.matches_played = 0 THEN NULL ELSE ts.glicko_rating END) DESC NULLS LAST
                )
            END AS rank
        FROM team_stats ts
        JOIN teams t ON t.id = ts.team_id
        WHERE ts.competition_id = :#{#competition.id}
          AND ts.ladder = :ladder
          AND t.is_deleted = false
        ORDER BY (ts.matches_played = 0) ASC, ts.glicko_rating DESC, t.id ASC
        """, nativeQuery = true)
    List<LeaderboardRow> findLeaderboardRowsByCompetitionAndLadder(@Param("competition") Competition competition, @Param("ladder") String ladder);
}
