// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

class ViewUtil
{

    static getView(element)
    {
        const proxy = element.getAttribute("data-proxy");
        if(proxy) element = document.getElementById(proxy);
        return EnumUtil.enumOfName(element.closest("[data-view-name]").getAttribute("data-view-name"), VIEW);
    }

}