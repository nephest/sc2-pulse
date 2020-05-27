// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

class Util
{

    static urlencodeFormData(fd)
    {
        let s = '';

        for(const pair of fd.entries()){
           if(typeof pair[1]=='string'){
               s += (s?'&':'') + Util.encodeSpace(pair[0])+'='+ Util.encodeSpace(pair[1]);
           }
        }
        return s;
    }

    static getFormParameters(page = 0)
    {
        const fd = new FormData(document.getElementById("form-ladder"));
        if (page >= 0) fd.set("page", page);
        return Util.urlencodeFormData(fd);
    }

    static setGeneratingStatus(status, errorText = "Error")
    {
        switch(status)
        {
            case "begin":
                Session.currentRequests++;
                if (Session.currentRequests > 1) return;
                Session.currentSeason = document.getElementById("form-ladder-season-picker").value;
                Session.currentTeamFormat = EnumUtil.enumOfFullName(document.getElementById("form-ladder-team-format-picker").value, TEAM_FORMAT);
                Session.currentTeamType = EnumUtil.enumOfName(document.getElementById("form-ladder-team-type-picker").value, TEAM_TYPE);
                ElementUtil.disableElements(document.getElementsByTagName("input"), true);
                ElementUtil.disableElements(document.getElementsByTagName("select"), true);
                ElementUtil.disableElements(document.getElementsByTagName("button"), true);
                PaginationUtil.setPaginationsState(false);
                ElementUtil.setElementsVisibility(document.getElementsByClassName("status-generating-begin"), true);
                ElementUtil.setElementsVisibility(document.getElementsByClassName("status-generating-success"), false);
                ElementUtil.setElementsVisibility(document.getElementsByClassName("status-generating-error"), false);
            break;
            case "success":
            case "error":
                Session.currentRequests--;
                if(status === "error")
                {
                    document.getElementById("error-generation-text").textContent = errorText;
                    $("#error-generation").modal();
                }
                if(Session.currentRequests > 0) return;
                ElementUtil.disableElements(document.getElementsByTagName("input"), false);
                ElementUtil.disableElements(document.getElementsByTagName("select"), false);
                ElementUtil.disableElements(document.getElementsByTagName("button"), false);
                PaginationUtil.setPaginationsState(true);
                ElementUtil.setElementsVisibility(document.getElementsByClassName("status-generating-begin"), false);
                ElementUtil.setElementsVisibility(document.getElementsByClassName("status-generating-" + status), true);
                Session.isHistorical = false;
            break;
        }
    }

    static getCookie(cname)
    {
        var name = cname + "=";
        var decodedCookie = decodeURIComponent(document.cookie);
        var ca = decodedCookie.split(';');
        for(var i = 0; i <ca.length; i++)
        {
            var c = ca[i];
            while (c.charAt(0) == ' ') c = c.substring(1);
            if (c.indexOf(name) == 0) return c.substring(name.length, c.length);
        }
        return "";
    }

    static compareValueArrays(a, b)
    {
        var compare = 0;
        for(var i = 0; i < a.length; i++)
        {
            compare = Util.compareValues(a[i], b[i]);
            if(compare !== 0) return compare;
        }
        return compare;
    }

    static compareValues(v1, v2)
    {
        return v1 !== '' && v2 !== '' && !isNaN(v1) && !isNaN(v2) ? v1 - v2 : v1.toString().localeCompare(v2);
    }

    static scrollIntoViewById(id)
    {
        document.getElementById(id).scrollIntoView();
    }

    static calculatePercentage(val, allVal)
    {
        return Math.round((val / allVal) * 100);
    }

    static hasNonZeroValues(values)
    {
        for (let i = 0; i < values.length; i++)
        {
            const val = values[i];
            if (!isNaN(val) && val != 0)
            {
                return true;
            }
        }
        return false;
    }

    static encodeSpace(s){ return encodeURIComponent(s).replace(/%20/g,'+'); }

    static calculateRank(searchResult, i)
    {
        return (searchResult.meta.page - 1) * searchResult.meta.perPage + i + 1;
    }

}