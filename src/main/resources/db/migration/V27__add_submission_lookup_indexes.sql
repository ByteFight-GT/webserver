BEGIN;

-- Supports looking up a submission's latest match in a given status (e.g. its validation result).
CREATE INDEX game_matches_submission_a_status_idx
    ON game_matches (submission_a_id, status, id);

-- Supports listing a team's submissions.
CREATE INDEX submissions_team_id_idx
    ON submissions (team_id);

END;
