BEGIN;
ALTER TABLE IF EXISTS public.users
    ADD COLUMN internal_comp_eligible boolean NOT NULL DEFAULT false;
END;
