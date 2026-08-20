BEGIN;
ALTER TABLE IF EXISTS public.competitions
    ADD COLUMN internal boolean NOT NULL DEFAULT false;

END;
