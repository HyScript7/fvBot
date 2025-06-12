/*
POSTGRESQL
Migration Script
 */
-- Tables
CREATE TABLE
    IF NOT EXISTS "level_role" (
        "id" BIGSERIAL PRIMARY KEY,
        "guild_id" BIGINT,
        "role_id" BIGINT UNIQUE,
        "level_threshold" INT,
        "remove_lower_roles" BOOLEAN
    );