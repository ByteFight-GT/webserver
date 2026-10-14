BEGIN;
ALTER TABLE IF EXISTS public.competitions
    ADD COLUMN leaderboard_pagination boolean NOT NULL DEFAULT false,
    ADD COLUMN leaderboard_page_size integer NOT NULL DEFAULT 25;

ALTER TABLE IF EXISTS public.competitions
    ADD CONSTRAINT competitions_leaderboard_page_size_positive
        CHECK (leaderboard_page_size >= 1 AND leaderboard_page_size <= 200);

END;
