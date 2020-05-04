package com.ggstudios.tools.datafixer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StaticDataUtil {

    class ChampionInfo {
        var name: String? = null
        var id: Int = 0
        var key: String? = null
    }

    fun makeChampionDictionary(version: String): Map<String, ChampionInfo> {
        val gson = Gson()
        val type = object : TypeToken<Map<String, ChampionInfo>>() {}.type

        val championJson = DataFetcher.client.getAllChampionsJsonDd(version)
        return gson.fromJson(championJson.getJSONObject("data").toString(), type)
    }
}