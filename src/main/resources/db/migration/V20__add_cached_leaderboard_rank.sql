BEGIN;

ALTER TABLE IF EXISTS public.teams
    ADD COLUMN cached_leaderboard_rank integer;

WITH ranked AS (
    SELECT
        ts.team_id,
        DENSE_RANK() OVER (
            PARTITION BY ts.competition_id
            ORDER BY ts.glicko_rating DESC
        ) AS rank
    FROM public.team_stats ts
    JOIN public.teams t ON t.id = ts.team_id
    WHERE ts.ladder = 'ranked'
      AND ts.matches_played > 0
      AND t.deleted_at IS NULL
)
UPDATE public.teams t
SET cached_leaderboard_rank = ranked.rank
FROM ranked
WHERE ranked.team_id = t.id;

CREATE INDEX teams_competition_cached_leaderboard_rank_idx
    ON public.teams (competition_id, cached_leaderboard_rank)
    WHERE cached_leaderboard_rank IS NOT NULL;

END;
