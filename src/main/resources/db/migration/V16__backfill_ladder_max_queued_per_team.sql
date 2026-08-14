-- Repairs ladders created before Ladder.maxQueuedPerTeam gained a default (webserver#159).
-- The column has carried DEFAULT 10 since V6, but Hibernate always wrote the field explicitly,
-- so every ladder created through the admin API landed on 0 and rejected user matches with 429.
-- 0 is not a valid configuration: allow_user_matches is the switch for disabling a ladder.
BEGIN;
UPDATE public.ladders
    SET max_queued_per_team = 10
    WHERE max_queued_per_team = 0;

END;
