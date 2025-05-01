/*
POSTGRESQL
Migration Script
*/

-- Tables
CREATE TABLE IF NOT EXISTS "member" (
    "id" BIGSERIAL PRIMARY KEY,
    "discord_id" BIGINT,
    "username" VARCHAR(255),
    "current_character_id" BIGINT
);

CREATE TABLE IF NOT EXISTS "character" (
    "id" BIGSERIAL PRIMARY KEY,
    "first_name" VARCHAR(255),
    "last_name" VARCHAR(255),
    "innate_name" VARCHAR(255),
    "title_id" BIGINT,
    "resurrection" INTEGER,
    "level" INTEGER,
    "experience" DOUBLE PRECISION,
    "user_id" BIGINT
);

CREATE TABLE IF NOT EXISTS "title" (
    "id" BIGSERIAL PRIMARY KEY,
    "prefix" VARCHAR(255),
    "suffix" VARCHAR(255)
);

-- N:N Join Tables
CREATE TABLE IF NOT EXISTS "usertitlegrant" (
    "user_id" BIGINT,
    "title_id" BIGINT
);
CREATE TABLE IF NOT EXISTS "charactertitlegrant" (
    "character_id" BIGINT,
    "title_id" BIGINT
);

-- FKs
ALTER TABLE "member" ADD CONSTRAINT fk_current_character_id FOREIGN KEY ("current_character_id") REFERENCES "character" ("id");
ALTER TABLE "character" ADD CONSTRAINT fk_title_id FOREIGN KEY ("title_id") REFERENCES "title" ("id");
ALTER TABLE "character" ADD CONSTRAINT fk_user_id FOREIGN KEY ("user_id") REFERENCES "member" ("id");
ALTER TABLE "usertitlegrant" ADD CONSTRAINT fk_user_grant_user_id FOREIGN KEY ("user_id") REFERENCES "member" ("id");
ALTER TABLE "usertitlegrant" ADD CONSTRAINT fk_user_grant_title_id FOREIGN KEY ("title_id") REFERENCES "title" ("id");
ALTER TABLE "charactertitlegrant" ADD CONSTRAINT fk_character_grant_character_id FOREIGN KEY ("character_id") REFERENCES "character" ("id");
ALTER TABLE "charactertitlegrant" ADD CONSTRAINT fk_character_grant_title_id FOREIGN KEY ("title_id") REFERENCES "title" ("id");

-- UKs
ALTER TABLE "member" ADD CONSTRAINT uk_discord_id UNIQUE ("discord_id");
ALTER TABLE "character" ADD CONSTRAINT uk_innate_name UNIQUE ("innate_name");
