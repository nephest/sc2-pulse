// Copyright (C) 2020-2021 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local.ladder.dao;

import com.nephest.battlenet.sc2.model.QueueType;
import com.nephest.battlenet.sc2.model.Race;
import com.nephest.battlenet.sc2.model.local.Clan;
import com.nephest.battlenet.sc2.model.local.League;
import com.nephest.battlenet.sc2.model.local.dao.AccountDAO;
import com.nephest.battlenet.sc2.model.local.dao.ClanDAO;
import com.nephest.battlenet.sc2.model.local.dao.PlayerCharacterDAO;
import com.nephest.battlenet.sc2.model.local.dao.SeasonDAO;
import com.nephest.battlenet.sc2.model.local.ladder.LadderDistinctCharacter;
import com.nephest.battlenet.sc2.model.util.PostgreSQLUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LadderCharacterDAO
{

    private static final String FIND_DISTINCT_CHARACTER_FORMAT =
    "WITH %2$s "
    + "player_character_filtered AS "
    + "( "
        + "SELECT player_character.id "
        + "FROM player_character "
        + "%1$s"
    + "), "
    + "player_character_stats_filtered AS "
    + "( "
        + "SELECT DISTINCT ON(player_character_stats.player_character_id) "
        + "player_character_stats.id, player_character_stats.player_character_id "
        + "FROM player_character_filtered "
        + "INNER JOIN player_character_stats ON player_character_stats.player_character_id = player_character_filtered.id "
        + "ORDER BY player_character_stats.player_character_id, player_character_stats.rating_max DESC, "
        + "player_character_stats.race, " //prevent summary selection where racial stats are available
        + "player_character_stats.games_played DESC, player_character_stats.league_max DESC, "
        + "player_character_stats.queue_type, player_character_stats.team_type "
    + "), "
    + "player_character_stats_current AS "
    + "( "
        + "SELECT DISTINCT ON(player_character_filtered.id) "
        + "player_character_filtered.id AS player_character_id, "
        + "MAX(rating) AS rating, "
        + "SUM(wins + losses + ties) AS games_played, "
        + "MAX(global_rank) AS global_rank "
        + "FROM player_character_filtered "
        + "INNER JOIN team_member ON player_character_filtered.id = team_member.player_character_id "
        + "INNER JOIN team ON team_member.team_id = team.id "
        + "WHERE team.season = :season "
        + "AND queue_type = :queueType "
        + "GROUP BY player_character_filtered.id"
    + ") "

    + "SELECT "
    + "pro_player.nickname AS \"pro_player.nickname\", "
    + "COALESCE(pro_team.short_name, pro_team.name) AS \"pro_player.team\","
    + AccountDAO.STD_SELECT + ", "
    + PlayerCharacterDAO.STD_SELECT + ", "
    + ClanDAO.STD_SELECT + ", "
    + "player_character_stats.race AS \"race\", "
    + "player_character_stats.league_max AS \"league_max\", "
    + "player_character_stats.rating_max AS \"rating_max\", "
    + "player_character_stats.games_played AS \"games_played\", "
    + "player_character_stats_current.rating as \"rating_cur\", "
    + "player_character_stats_current.games_played as \"games_played_cur\", "
    + "player_character_stats_current.global_rank as \"rank_cur\" "

    + "FROM player_character_stats_filtered "
    + "INNER JOIN player_character ON player_character.id = player_character_stats_filtered.player_character_id "
    + "INNER JOIN account ON player_character.account_id = account.id "
    + "INNER JOIN player_character_stats ON player_character_stats_filtered.id = player_character_stats.id "
    + "LEFT JOIN clan ON player_character.clan_id = clan.id "
    + "LEFT JOIN pro_player_account ON account.id=pro_player_account.account_id "
    + "LEFT JOIN pro_player ON pro_player_account.pro_player_id=pro_player.id "
    + "LEFT JOIN pro_team_member ON pro_player.id=pro_team_member.pro_player_id "
    + "LEFT JOIN pro_team ON pro_team_member.pro_team_id=pro_team.id "
    + "LEFT JOIN player_character_stats_current ON player_character_stats_current.player_character_id = player_character_stats_filtered.player_character_id "

    + "ORDER BY rating_cur DESC NULLS LAST, rating_max DESC";

    private static final String FIND_DISTINCT_CHARACTER_BY_NAME_OR_BATTLE_TAG_OR_PRO_NICKNAME_QUERY = String.format
    (
        FIND_DISTINCT_CHARACTER_FORMAT,
        "WHERE LOWER(player_character.name) LIKE LOWER(:likeName) "
        + "UNION "
        + "SELECT player_character.id "
        + "FROM player_character "
        + "INNER JOIN account ON player_character.account_id = account.id "
        + "WHERE LOWER(account.battle_tag) LIKE LOWER(:likeName) "
        + "UNION "
        + "SELECT player_character.id "
        + "FROM player_character "
        + "INNER JOIN account ON player_character.account_id = account.id "
        + "LEFT JOIN pro_player_account ON account.id = pro_player_account.account_id "
        + "LEFT JOIN pro_player ON pro_player_account.pro_player_id=pro_player.id "
        + "WHERE LOWER(pro_player.nickname)=LOWER(:name)", ""
    );
    private static final String FIND_DISTINCT_CHARACTER_BY_FULL_BATTLE_TAG_QUERY = String.format
    (
        FIND_DISTINCT_CHARACTER_FORMAT,
        "INNER JOIN account ON player_character.account_id = account.id "
        + "WHERE account.battle_tag = :battleTag ", ""
    );
    private static final String FIND_DISTINCT_CHARACTER_BY_ACCOUNT_ID_QUERY = String.format
    (
        FIND_DISTINCT_CHARACTER_FORMAT,
        "INNER JOIN account ON player_character.account_id=account.id "
        + "WHERE account.id = :accountId ", ""
    );
    private static final String FIND_DISTINCT_CHARACTER_BY_PROFILE_LINK_QUERY = String.format
    (
        FIND_DISTINCT_CHARACTER_FORMAT,
        "WHERE region = :region "
        + "AND realm = :realm "
        + "AND battlenet_id = :battlenetId", ""
    );

    private static final String FIND_LINKED_DISTINCT_CHARACTERS_TEMPLATE = String.format
    (
        FIND_DISTINCT_CHARACTER_FORMAT,
        "INNER JOIN account ON player_character.account_id=account.id "
        + "INNER JOIN account_filtered ON account_filtered.id=account.id "
        + "UNION "
        + "SELECT player_character.id "
        + "FROM pro_player_filtered "
        + "INNER JOIN pro_player_account ON pro_player_filtered.id = pro_player_account.pro_player_id "
        + "INNER JOIN account ON pro_player_account.account_id = account.id "
        + "INNER JOIN player_character ON account.id = player_character.account_id",

        "pro_player_filtered AS "
        + "("
            + "SELECT pro_player.id FROM pro_player "
            + "INNER JOIN pro_player_account ON pro_player.id = pro_player_account.pro_player_id "
            + "INNER JOIN account ON pro_player_account.account_id=account.id "
            + "%1$s"
        + "),"
        + "account_filtered AS "
        + "("
            + "SELECT account.id FROM account "
            + "%1$s"
        + "),"
    );

    private static final String FIND_LINKED_DISTINCT_CHARACTERS_BY_PLAYER_CHARACTER_ID_QUERY = String.format
    (
        FIND_LINKED_DISTINCT_CHARACTERS_TEMPLATE,
        "INNER JOIN player_character ON account.id = player_character.account_id "
        + "WHERE player_character.id = :playerCharacterId"
    );

    private static final String FIND_LINKED_DISTINCT_CHARACTERS_BY_ACCOUNT_ID_QUERY = String.format
    (
        FIND_LINKED_DISTINCT_CHARACTERS_TEMPLATE,
        "WHERE account.id = :accountId"
    );

    public static final QueueType CURRENT_STATS_QUEUE_TYPE = QueueType.LOTV_1V1;

    private final NamedParameterJdbcTemplate template;
    private final ConversionService conversionService;
    private final SeasonDAO seasonDAO;

    private final RowMapper<LadderDistinctCharacter> DISTINCT_CHARACTER_ROW_MAPPER;
    private final ResultSetExtractor<LadderDistinctCharacter> DISTINCT_CHARACTER_EXTRACTOR;

    @Autowired
    public LadderCharacterDAO
    (
        @Qualifier("sc2StatsNamedTemplate") NamedParameterJdbcTemplate template,
        @Qualifier("sc2StatsConversionService") ConversionService conversionService,
        SeasonDAO seasonDAO
    )
    {
        this.template = template;
        this.conversionService = conversionService;
        this.seasonDAO = seasonDAO;
        DISTINCT_CHARACTER_ROW_MAPPER =
        (rs, num)->
        {
            Integer gamesPlayed = rs.getInt("games_played");
            int raceInt = rs.getInt("race");
            Race race = rs.wasNull() ? null : conversionService.convert(raceInt, Race.class);
            int mmrCurrentInt = rs.getInt("rating_cur");
            mmrCurrentInt = rs.wasNull() ? -1 : mmrCurrentInt;
            int gamesPlayedCurrent = rs.getInt("games_played_cur");
            gamesPlayedCurrent = rs.wasNull() ? -1 : gamesPlayedCurrent;
            int rankCurrent = rs.getInt("rank_cur");
            rankCurrent = rs.wasNull() ? -1 : rankCurrent;
            rs.getInt("clan.id");
            Clan clan = rs.wasNull() ? null : ClanDAO.getStdRowMapper().mapRow(rs, num);
            return new LadderDistinctCharacter
            (
                conversionService.convert(rs.getInt("league_max"), League.LeagueType.class),
                rs.getInt("rating_max"),
                mmrCurrentInt == -1 ? null : mmrCurrentInt,
                AccountDAO.getStdRowMapper().mapRow(rs, num),
                PlayerCharacterDAO.getStdRowMapper().mapRow(rs, num),
                clan,
                rs.getString("pro_player.nickname"),
                rs.getString("pro_player.team"),
                race == Race.TERRAN ? gamesPlayed : null,
                race == Race.PROTOSS ? gamesPlayed : null,
                race == Race.ZERG ? gamesPlayed : null,
                race == Race.RANDOM ? gamesPlayed : null,
                gamesPlayed,
                gamesPlayedCurrent == -1 ? null : gamesPlayedCurrent,
                rankCurrent == -1 ? null : rankCurrent
            );
        };
        DISTINCT_CHARACTER_EXTRACTOR = (rs)->{
            if(!rs.next()) return null;
            return DISTINCT_CHARACTER_ROW_MAPPER.mapRow(rs, 0);
        };
    }

    public List<LadderDistinctCharacter> findDistinctCharacters(String term)
    {
        if(term.contains("#")) return findDistinctCharactersByFullBattleTag(term);
        if(term.contains("/")) {
            LadderDistinctCharacter c = findDistinctCharacterByProfileLink(term).orElse(null);
            return c == null ? List.of() : List.of(c);
        }
        if(term.equalsIgnoreCase("f")) return List.of(); //forbid fake battletags/names
        return findDistinctCharactersByNameOrBattletagOrProNickname(term);
    }

    private List<LadderDistinctCharacter> findDistinctCharactersByNameOrBattletagOrProNickname(String name)
    {
        String likeName = PostgreSQLUtils.escapeLikePattern(name) + "#%";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("name", name)
            .addValue("likeName", likeName)
            .addValue("season", seasonDAO.getMaxBattlenetId())
            .addValue("queueType", conversionService.convert(CURRENT_STATS_QUEUE_TYPE, Integer.class));
        return template
            .query(FIND_DISTINCT_CHARACTER_BY_NAME_OR_BATTLE_TAG_OR_PRO_NICKNAME_QUERY, params, DISTINCT_CHARACTER_ROW_MAPPER);
    }

    private List<LadderDistinctCharacter> findDistinctCharactersByFullBattleTag(String battleTag)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("battleTag", battleTag)
            .addValue("season", seasonDAO.getMaxBattlenetId())
            .addValue("queueType", conversionService.convert(CURRENT_STATS_QUEUE_TYPE, Integer.class));
        return template
            .query(FIND_DISTINCT_CHARACTER_BY_FULL_BATTLE_TAG_QUERY, params, DISTINCT_CHARACTER_ROW_MAPPER);
    }

    public List<LadderDistinctCharacter> findDistinctCharactersByAccountId(Long accountId)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("accountId", accountId)
            .addValue("season", seasonDAO.getMaxBattlenetId())
            .addValue("queueType", conversionService.convert(CURRENT_STATS_QUEUE_TYPE, Integer.class));
        return template
            .query(FIND_DISTINCT_CHARACTER_BY_ACCOUNT_ID_QUERY, params, DISTINCT_CHARACTER_ROW_MAPPER);
    }

    public Optional<LadderDistinctCharacter> findDistinctCharacterByProfileLink(String profile)
    {
        String[] split = profile.split("/");
        if(split.length < 3) throw new IllegalArgumentException("Invalid profile link");
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("region", Integer.parseInt(split[split.length - 3]))
            .addValue("realm", Integer.parseInt(split[split.length - 2]))
            .addValue("battlenetId", Long.parseLong(split[split.length - 1]))
            .addValue("season", seasonDAO.getMaxBattlenetId())
            .addValue("queueType", conversionService.convert(CURRENT_STATS_QUEUE_TYPE, Integer.class));
        return Optional.ofNullable(template.query(FIND_DISTINCT_CHARACTER_BY_PROFILE_LINK_QUERY, params, DISTINCT_CHARACTER_EXTRACTOR));
    }

    public List<LadderDistinctCharacter> findLinkedDistinctCharactersByCharacterId(Long playerCharacterId)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("playerCharacterId", playerCharacterId)
            .addValue("season", seasonDAO.getMaxBattlenetId())
            .addValue("queueType", conversionService.convert(CURRENT_STATS_QUEUE_TYPE, Integer.class));
        return template
            .query(FIND_LINKED_DISTINCT_CHARACTERS_BY_PLAYER_CHARACTER_ID_QUERY, params, DISTINCT_CHARACTER_ROW_MAPPER);
    }

    public List<LadderDistinctCharacter> findLinkedDistinctCharactersByAccountId(Long accountId)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("accountId", accountId)
            .addValue("season", seasonDAO.getMaxBattlenetId())
            .addValue("queueType", conversionService.convert(CURRENT_STATS_QUEUE_TYPE, Integer.class));
        return template
            .query(FIND_LINKED_DISTINCT_CHARACTERS_BY_ACCOUNT_ID_QUERY, params, DISTINCT_CHARACTER_ROW_MAPPER);
    }

}
