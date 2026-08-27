BEGIN;

ALTER TABLE IF EXISTS public.users
    ADD COLUMN last_accepted_tos timestamp NOT NULL DEFAULT TIMESTAMP 'epoch';

END;
