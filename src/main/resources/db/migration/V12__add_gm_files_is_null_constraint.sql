/*
 If team_id is null, the constraint of uniqueness on the (game_match_id, slug) pair is not enforced, which can lead
 to multiple game_match files uploaded for a single slug when the team_id is null.

 DBML isn't expressive enough to include the below constraint, so I'm adding it as a manual migration.
 */

CREATE UNIQUE INDEX game_match_files_null_team_unique
    ON game_match_files (game_match_id, slug)
    WHERE team_id IS NULL;