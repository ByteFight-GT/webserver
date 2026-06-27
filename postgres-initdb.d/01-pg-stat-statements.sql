-- This script only runs on first container initialization (empty data directory).
-- For existing production deployments, connect to the database and run:
--   CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
--   GRANT pg_read_all_stats TO <grafana_db_user>;

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
