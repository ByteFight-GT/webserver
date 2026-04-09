-- Used for correcting team_stats statistics from game_match table in case of an inconsistency.
-- This shouldn't need to be run very often, and an inconsistency almost always means an error in our code.

BEGIN;

WITH wdl AS (
    SELECT
        team_id,
        ladder,
        COUNT(*) AS matches_played,
        SUM(win)  AS wins,
        SUM(loss) AS losses,
        SUM(draw) AS draws
    FROM (
             SELECT
                 team_a_id AS team_id,
                 ladder,
                 CASE WHEN status = 'team_a_win' THEN 1 ELSE 0 END AS win,
                 CASE WHEN status = 'team_b_win' THEN 1 ELSE 0 END AS loss,
                 CASE WHEN status = 'draw' THEN 1 ELSE 0 END AS draw
             FROM game_matches
             WHERE team_a_id != team_b_id

             UNION ALL

             SELECT
                 team_b_id AS team_id,
                 ladder,
                 CASE WHEN status = 'team_b_win' THEN 1 ELSE 0 END AS win,
                 CASE WHEN status = 'team_a_win' THEN 1 ELSE 0 END AS loss,
                 CASE WHEN status = 'draw' THEN 1 ELSE 0 END AS draw
             FROM game_matches
             WHERE team_a_id != team_b_id
         ) t
    WHERE (win > 0 OR loss > 0 OR draw > 0)
    GROUP BY team_id, ladder
)

UPDATE team_stats
SET
    matches_played = wdl.matches_played,
    wins = wdl.wins,
    losses = wdl.losses,
    draws = wdl.draws
FROM wdl
WHERE wdl.team_id = team_stats.team_id
  AND wdl.ladder = team_stats.ladder
  AND team_stats.ladder IN ('ranked', 'scrimmage');

SELECT * FROM public.team_stats