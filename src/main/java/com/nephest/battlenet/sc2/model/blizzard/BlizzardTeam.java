// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.blizzard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nephest.battlenet.sc2.model.BaseTeam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@JsonNaming(SnakeCaseStrategy.class)
public class BlizzardTeam
extends BaseTeam
{

    private static final BlizzardTeamMember[] EMPTY_TEAM_MEMBER_ARRAY = new BlizzardTeamMember[0];

    public static final Long RATING_MAX = 20000l;

    @NotNull
    private BigInteger id;

    //members can be empty
    @Valid @NotNull
    @JsonProperty("member")
    private BlizzardTeamMember[] members = EMPTY_TEAM_MEMBER_ARRAY;

    /*
        blizzard normally returns zeros, not nulls.
        in a very rare occasion some of the values could be null.
        defaulting to zeros in this case
    */
    public BlizzardTeam(){super(0l, 0, 0, 0, 0);}

    public BlizzardTeam
    (
        BigInteger id, BlizzardTeamMember[] members,
        Long rating, Integer wins, Integer loses, Integer ties, Integer points
    )
    {
        /*
            blizzard normally returns zeros, not nulls.
            in a very rare occasion some of the values could be nulls
        */
        super
        (
            rating,
            (wins == null ? 0 : wins),
            (loses == null ? 0 : loses),
            (ties == null ? 0 : ties),
            (points == null ? 0 : points)
        );
        this.id = id;
        this.members = members;
    }

    public void setId(BigInteger id)
    {
        this.id = id;
    }

    public BigInteger getId()
    {
        return id;
    }

    public void setMembers(BlizzardTeamMember[] members)
    {
        this.members = members;
    }

    public BlizzardTeamMember[] getMembers()
    {
        return members;
    }

    //Blizzard can return unobtainable values here(cheaters?).
    //Reducing them to zero to avoid ladder pollution
    @Override
    public void setRating(Long rating)
    {
        if(rating == null || rating > RATING_MAX) rating = 0l;
        super.setRating(rating);
    }

    @Override
    public void setWins(Integer wins)
    {
        if(wins == null) wins = 0;
        super.setWins(wins);
    }

    @Override
    public void setLosses(Integer losses)
    {
        if(losses == null) losses = 0;
        super.setLosses(losses);
    }

    @Override
    public void setTies(Integer ties)
    {
        if(ties == null) ties = 0;
        super.setTies(ties);
    }

    @Override
    public void setPoints(Integer points)
    {
        if(points == null) points = 0;
        super.setPoints(points);
    }

}
