-- Adds an optional avatar (profile picture) file reference to players.
-- Mirrors the resume_file column added to users in V11.
BEGIN;
ALTER TABLE IF EXISTS public.players
    ADD COLUMN avatar_file bigint;
ALTER TABLE IF EXISTS public.players
    ADD CONSTRAINT players_avatar_file_key UNIQUE (avatar_file);
ALTER TABLE IF EXISTS public.players
    ADD CONSTRAINT players_avatar_file_fkey FOREIGN KEY (avatar_file)
        REFERENCES public.file_records (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        DEFERRABLE;

END;
