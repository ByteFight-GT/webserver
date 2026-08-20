BEGIN;
ALTER TABLE IF EXISTS public.users
    ADD COLUMN is_service_account boolean NOT NULL DEFAULT false;
END;
