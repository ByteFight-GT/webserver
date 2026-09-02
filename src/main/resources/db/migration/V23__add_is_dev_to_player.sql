BEGIN;
ALTER TABLE IF EXISTS public.players
    ADD COLUMN is_dev boolean NOT NULL DEFAULT false;
END;
