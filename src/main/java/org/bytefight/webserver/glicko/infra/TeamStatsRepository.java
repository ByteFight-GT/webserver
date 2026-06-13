package org.bytefight.webserver.glicko.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.domain.TeamRankRow;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.domain.TeamStatsAggregate;
import org.bytefight.webserver.leaderboard.domain.LeaderboardRow;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats, Long> {
  Optional<TeamStats> findByTeamAndLadder(Team team, String ladder);

  List<TeamStats> findAllByCompetitionAndLadderAndTeamIn(
      Competition competition, String ladder, Collection<Team> teams);

  List<TeamStats> findAllByCompetitionAndLadderOrderByGlickoRatingDesc(
      Competition competition, String ladder);

  @Query(
      """
    SELECT
        t.team as team,
        SUM(t.matchesPlayed) as matchesPlayed,
        SUM(t.wins) as wins,
        SUM(t.losses) as losses,
        SUM(t.draws) as draws
    FROM TeamStats t
    WHERE t.team = :team AND t.ladder IN :ladders
    GROUP BY t.team
""")
  TeamStatsAggregate getTeamStatsAggregateByTeamAndLadder(
      @Param("team") Team team, @Param("ladders") Collection<String> ladders);

  @Query(
      value =
          """
        SELECT ladder, rank FROM (
          SELECT
            ts.ladder AS ladder,
            ts.team_id AS team_id,
            CASE
              WHEN ts.matches_played = 0 THEN NULL
              ELSE DENSE_RANK() OVER (
                PARTITION BY ts.ladder
                ORDER BY (CASE WHEN ts.matches_played = 0 THEN NULL ELSE ts.glicko_rating END) DESC NULLS LAST
              )
            END AS rank
          FROM team_stats ts
          JOIN teams t ON t.id = ts.team_id AND t.is_deleted = false
          WHERE ts.competition_id = :competitionId
        ) AS subquery
        WHERE team_id = :teamId
        """,
      nativeQuery = true)
  List<TeamRankRow> findTeamRanksByCompetition(
      @Param("competitionId") Long competitionId, @Param("teamId") Long teamId);

  @Query(
      value =
          """
        SELECT
            t.uuid::text AS teamUuid,
            t.name AS teamName,
            t.quote AS teamQuote,
            t.type::text AS teamType,
            ts.glicko_rating AS glickoRating,
            t.display_members AS teamDisplayMembers,
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
          AND t.deleted_at IS NULL
        ORDER BY (ts.matches_played = 0) ASC, ts.glicko_rating DESC, t.id ASC
        """,
      nativeQuery = true)
  List<LeaderboardRow> findLeaderboardRowsByCompetitionAndLadder(
      @Param("competition") Competition competition, @Param("ladder") String ladder);
}
