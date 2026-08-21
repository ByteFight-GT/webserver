BEGIN;

ALTER TABLE IF EXISTS public.players
    ADD COLUMN full_name varchar(100),
    ADD COLUMN description varchar(512),
    ADD COLUMN school varchar(150),
    ADD COLUMN major varchar(256),
    ADD COLUMN github_link varchar(500),
    ADD COLUMN linkedin_link varchar(500),
    ADD COLUMN website_link varchar(500);

END;
