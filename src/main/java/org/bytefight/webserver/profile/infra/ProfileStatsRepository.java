package org.bytefight.webserver.profile.infra;

import java.util.Optional;

import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.profile.domain.ProfileStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileStatsRepository extends JpaRepository<ProfileStats, Long> {
  Optional<ProfileStats> findByPlayer(Player player);

  @Modifying(clearAutomatically = true)
  @Query(
      value =
          """
          WITH counts AS (
              SELECT p.id AS player_id, COUNT(gm.id) AS games_played
              FROM players p
              LEFT JOIN team_members tm ON tm.player_id = p.id
              LEFT JOIN teams t ON t.id = tm.team_id AND t.deleted_at IS NULL
              LEFT JOIN game_matches gm
                     ON (gm.team_a_id = t.id OR gm.team_b_id = t.id)
                    AND gm.status IN ('team_a_win', 'team_b_win', 'draw')
              GROUP BY p.id
          ),
          ranked AS (
              SELECT c.player_id,
                     CUME_DIST() OVER (ORDER BY c.games_played DESC) AS games_played_percentile
              FROM counts c
              WHERE c.games_played > 0
          )
          INSERT INTO profile_stats (player_id, games_played, games_played_percentile)
          SELECT c.player_id, c.games_played, r.games_played_percentile
          FROM counts c
          LEFT JOIN ranked r ON r.player_id = c.player_id
          ON CONFLICT (player_id) DO UPDATE
          SET games_played = EXCLUDED.games_played,
              games_played_percentile = EXCLUDED.games_played_percentile,
              updated_at = now()
          """,
      nativeQuery = true)
  int recomputeGamesPlayed();
}
