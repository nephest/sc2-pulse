-- Copyright (C) 2020 Oleksandr Masniuk and contributors
-- SPDX-License-Identifier: AGPL-3.0-or-later
---

ALTER TABLE "player_character_stats"
    ADD COLUMN "updated" TIMESTAMP NOT NULL DEFAULT NOW();

CREATE INDEX "ix_player_character_stats_calculation"
    ON "player_character_stats"("player_character_id", "race", "queue_type", "team_type", "season_id");

ALTER TABLE "team"
ADD COLUMN "global_rank" INTEGER NOT NULL DEFAULT 2147483647,
ADD COLUMN "region_rank" INTEGER NOT NULL DEFAULT 2147483647,
ADD COLUMN "league_rank" INTEGER NOT NULL DEFAULT 2147483647,
ADD COLUMN "tier_rank" INTEGER NOT NULL DEFAULT 2147483647,
ADD COLUMN "division_rank" SMALLINT NOT NULL DEFAULT 32767;