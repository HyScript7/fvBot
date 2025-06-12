/*
H2
Migration Script
 */
-- Tables
CREATE TABLE
    IF NOT EXISTS LevelRole (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        guild_id BIGINT,
        role_id BIGINT UNIQUE,
        level_threshold INT,
        remove_lower_roles BOOLEAN
    );
