SELECT installed_rank, version, description, type, script, installed_on, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
