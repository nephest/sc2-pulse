// Copyright (C) 2020-2021 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public class LeagueStats
extends BaseLocalTeamMember
implements java.io.Serializable
{

    private static final long serialVersionUID = 2L;

    @NotNull
    private Integer leagueId;

    @NotNull
    private Integer teamCount;

    public LeagueStats
    (
        Integer leagueId,
        Integer teamCount,
        Integer terranGamesPlayed,
        Integer protossGamesPlayed,
        Integer zergGamesPlayed,
        Integer randomGamesPlayed
    )
    {
        super(terranGamesPlayed, protossGamesPlayed, zergGamesPlayed, randomGamesPlayed);
        this.leagueId = leagueId;
        this.teamCount = teamCount;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getLeagueId());
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null) return false;
        if (other == this) return true;
        if ( !(other instanceof LeagueStats) ) return false;

        LeagueStats otherStats = (LeagueStats) other;
        return getLeagueId() != null
            && getLeagueId().equals(otherStats.getLeagueId());
    }

    @Override
    public String toString()
    {
        return String.format
        (
            "%s[%s]",
            LeagueStats.class.getSimpleName(),
            getLeagueId()
        );
    }

    public void setLeagueId(Integer leagueId)
    {
        this.leagueId = leagueId;
    }

    public Integer getLeagueId()
    {
        return leagueId;
    }

    public void setTeamCount(Integer teamCount)
    {
        this.teamCount = teamCount;
    }

    public Integer getTeamCount()
    {
        return teamCount;
    }

}
