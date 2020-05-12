// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.blizzard;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nephest.battlenet.sc2.model.BaseAccount;

import javax.validation.constraints.NotNull;

@JsonNaming(SnakeCaseStrategy.class)
public class BlizzardAccount
extends BaseAccount
{

    @NotNull
    private Long id;

    public BlizzardAccount(){}

    public BlizzardAccount
    (
        Long id,
        String battleTag
    )
    {
        super(battleTag);
        this.id = id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

}
