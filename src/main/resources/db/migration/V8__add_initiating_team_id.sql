ALTER TABLE IF EXISTS public.game_matches
    ADD COLUMN initiating_team_id bigint;

ALTER TABLE IF EXISTS public.game_matches
    ADD CONSTRAINT game_matches_initiating_team_id_fkey FOREIGN KEY (initiating_team_id)
        REFERENCES public.teams (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        DEFERRABLE;