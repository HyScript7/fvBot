/*
POSTGRESQL
Migration Script
*/

-- Tables
CREATE TABLE IF NOT EXISTS "Member" (
    "id" BIGSERIAL PRIMARY KEY,
    "discord_id" BIGINT,
    "username" VARCHAR(255),
    "current_character_id" BIGINT,
);

CREATE TABLE IF NOT EXISTS "Character" (
    "id" BIGSERIAL PRIMARY KEY,
    "first_name" VARCHAR(255),
    "last_name" VARCHAR(255),
    "innate_name" VARCHAR(255),
    "title_id" BIGINT,
    "level" INTEGER,
    "experience" DOUBLE PRECISION,
    "user_id" BIGINT
);

CREATE TABLE IF NOT EXISTS "Title" (
    "id" BIGSERIAL PRIMARY KEY,
    "prefix" VARCHAR(255),
    "suffix" VARCHAR(255)
);

-- N:N Join Tables
CREATE TABLE IF NOT EXISTS "UserTitleGrant" (
    "user_id" BIGINT,
    "title_id" BIGINT
);
CREATE TABLE IF NOT EXISTS "CharacterTitleGrant" (
    "character_id" BIGINT,
    "title_id" BIGINT
);

-- FKs
ALTER TABLE "Member" ADD CONSTRAINT fk_current_character_id FOREIGN KEY ("current_character_id") REFERENCES "Character" ("id");
ALTER TABLE "Character" ADD CONSTRAINT fk_title_id FOREIGN KEY ("title_id") REFERENCES "Title" ("id");
ALTER TABLE "Character" ADD CONSTRAINT fk_user_id FOREIGN KEY ("user_id") REFERENCES "Member" ("id");
ALTER TABLE "UserTitleGrant" ADD CONSTRAINT fk_user_grant_user_id FOREIGN KEY ("user_id") REFERENCES "Member" ("id");
ALTER TABLE "UserTitleGrant" ADD CONSTRAINT fk_user_grant_title_id FOREIGN KEY ("title_id") REFERENCES "Title" ("id");
ALTER TABLE "CharacterTitleGrant" ADD CONSTRAINT fk_character_grant_character_id FOREIGN KEY ("character_id") REFERENCES "Character" ("id");
ALTER TABLE "CharacterTitleGrant" ADD CONSTRAINT fk_character_grant_title_id FOREIGN KEY ("title_id") REFERENCES "Title" ("id");

-- UKs
ALTER TABLE "Member" ADD CONSTRAINT uk_discord_id UNIQUE ("discord_id");
ALTER TABLE "Character" ADD CONSTRAINT uk_innate_name UNIQUE ("innate_name");
