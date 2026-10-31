BEGIN;
ALTER TABLE IF EXISTS public.game_matches
    ADD COLUMN retries integer NOT NULL DEFAULT 0;
END;
