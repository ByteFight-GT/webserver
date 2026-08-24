BEGIN;

ALTER TABLE game_matches
    ADD COLUMN map_code varchar(100),
    ADD COLUMN outcome_reason_code varchar(100);

CREATE INDEX game_matches_competition_map_code_idx
    ON game_matches (competition_id, map_code)
    WHERE map_code IS NOT NULL;

CREATE INDEX game_matches_competition_outcome_reason_code_idx
    ON game_matches (competition_id, outcome_reason_code)
    WHERE outcome_reason_code IS NOT NULL;

END;
