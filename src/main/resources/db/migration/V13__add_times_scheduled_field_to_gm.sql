BEGIN;
ALTER TABLE IF EXISTS public.game_matches
    ADD COLUMN times_scheduled integer NOT NULL DEFAULT 0;
END;