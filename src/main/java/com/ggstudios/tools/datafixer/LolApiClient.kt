package com.ggstudios.tools.datafixer

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.HashMap

class LolApiClient (
        private val region: String,
        private val apiKey: String?
) {
    //public static final String REGION_NA = "na1";

    companion object {

        @Suppress("unused")
        private val TAG = LolApiClient::class.java.simpleName

        // https://na.api.pvp.net/api/lol/static-data/na/v1.2/champion?api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

        private const val URL_TEMPLATE = "https://%s.api.riotgames.com/lol/static-data/v3/%s?%s"

        private const val REQUEST_CHAMPION_SPECIFIC = "champions/%d"
        private const val REQUEST_ITEM_INFO = "items"
        private const val REQUEST_RUNE_INFO = "runes"
        private const val REQUEST_SUMMONER_SPELL_INFO = "summoner-spells"
        private const val REQUEST_MASTERY_INFO = "masteries"
        private const val REQUEST_LANGUAGES = "languages"

        const val KEY_LOCALE = "locale"
        const val KEY_API_KEY = "api_key"
        const val KEY_CHAMPION_DATA = "champData"
        const val KEY_VERSION = "version"
        const val KEY_ITEM_LIST_DATA = "itemListData"
        const val KEY_RUNE_LIST_DATA = "runeListData"
        const val KEY_SUMMONER_SPELL_DATA = "spellListData"
        const val KEY_MASTERY_LIST_DATA = "masteryListData"

        const val REGION_NA = "euw1"

        val INSTANCE = LolApiClient(REGION_NA, null)
    }

    val versionsDd: JSONArray
        @Throws(IOException::class, JSONException::class)
        get() {
            val url = "https://ddragon.leagueoflegends.com/api/versions.json"
            return JSONArray(makeRequestRaw(url, 0)!!)
        }

    val languages: JSONArray
        @Throws(IOException::class)
        get() {
            val arr = arrayOf(arrayOf(KEY_API_KEY, apiKey))

            return JSONArray(makeRequest(REQUEST_LANGUAGES, makeExtras(arr))!!)
        }

    private fun makeExtras(kvPairs: Array<out Array<out String?>>): String {
        val builder = StringBuilder()
        for (pair in kvPairs) {
            if (pair.size == 2) {
                // remove all empty pairs...
                if (pair[1] == null) continue
            }
            if (builder.isNotEmpty()) {
                builder.append('&')
            }

            builder.append(pair[0])
            builder.append('=')

            for (i in 1 until pair.size) {
                if (i != 1) {
                    builder.append(',')
                }
                builder.append(pair[i])
            }

        }
        return builder.toString()
    }

    @Throws(IOException::class)
    fun getAllChampionsFullJsonDd(version: String, locale: String? = "en_US"): JSONObject {
        val locale = locale ?: "en_US"
        val url = "http://ddragon.leagueoflegends.com/cdn/$version/data/$locale/championFull.json"
        return swapIdWithKey(JSONObject(makeRequestRaw(url, 0)!!))
    }

    @Throws(IOException::class)
    fun getAllChampionsJsonDd(version: String, locale: String? = "en_US"): JSONObject {
        val locale = locale ?: "en_US"
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/champion.json",
                version,
                locale)

        return swapIdWithKey(JSONObject(makeRequestRaw(url, 0)!!))
    }

    @Throws(IOException::class)
    fun getChampionJsonDd(version: String, locale: String? = "en_US", championKey: String): JSONObject {
        val locale = locale ?: "en_US"
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/champion/%s.json",
                version,
                locale,
                championKey)

        val jsonObject = JSONObject(makeRequestRaw(url, 0)!!)
        val data = jsonObject.getJSONObject("data")
        val championData = data.getJSONObject(championKey)
        val id = championData.getInt("key")

        championData.put("key", championKey)
        championData.put("id", id)

        return championData
    }

    @Throws(IOException::class)
    fun getAllItemJsonDd(version: String, locale: String? = "en_US"): JSONObject {
        val locale = locale ?: "en_US"
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/item.json",
                version,
                locale)
        return JSONObject(makeRequestRaw(url, 0)!!)
    }

    /**
     * Gets rune info.
     * @param version version of the rune info to fetch. Pass null for latest version.
     * @param locale
     */
    @Throws(IOException::class, JSONException::class)
    fun getAllRuneJson(version: String, locale: String): JSONObject {
        val arr = arrayOf(arrayOf(KEY_API_KEY, apiKey), arrayOf(KEY_RUNE_LIST_DATA, "all"), arrayOf(KEY_LOCALE, locale), arrayOf(KEY_VERSION, version))

        return JSONObject(makeRequest(REQUEST_RUNE_INFO, makeExtras(arr))!!)
    }

    @Throws(IOException::class, JSONException::class)
    fun getAllRuneJsonDd(version: String, locale: String? = "en_US"): JSONObject {
        val locale = locale ?: "en_US"
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/rune.json",
                version,
                locale)

        val jsonObject = JSONObject(makeRequestRaw(url, 0)!!)
        val data = jsonObject.getJSONObject("data")

        val keys = data.keys()
        while (keys.hasNext()) {
            val strId = keys.next()
            val id = Integer.parseInt(strId)
            data.getJSONObject(strId).put("id", id)
        }

        return jsonObject
    }

    @Throws(IOException::class, JSONException::class)
    fun getAllSummonerJsonDd(version: String, locale: String?): JSONObject {
        val locale = locale ?: "en_US"
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/summoner.json",
                version,
                locale)

        return swapIdWithKey(JSONObject(makeRequestRaw(url, 0)!!))
    }

    @Throws(IOException::class)
    fun getAllPerksJsonDd(version: String, locale: String?): JSONObject {
        val locale = locale ?: "en_US"
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/runesReforged.json",
                version,
                locale)

        val json = JSONObject()
        val jsonArr = JSONArray(makeRequestRaw(url, 0)!!)
        json.put("data", jsonArr)

        return json
    }

    @Throws(IOException::class, JSONException::class)
    fun getAllMasteryJsonDd(version: String, locale: String = "en_US"): JSONObject {
        val url = String.format("http://ddragon.leagueoflegends.com/cdn/%s/data/%s/mastery.json",
                version,
                locale)
        val jsonObject = JSONObject(makeRequestRaw(url, 0)!!)

        val idToTreeName = HashMap<Int, String>()

        // fix the dd data
        run {
            val trees = jsonObject.getJSONObject("tree")
            val keys = trees.keys()
            while (keys.hasNext()) {
                val treeName = keys.next()
                val tree = trees.getJSONArray(treeName)
                val len = tree.length()
                for (i in 0 until len) {
                    val arr = tree.getJSONArray(i)
                    val len2 = arr.length()
                    for (j in 0 until len2) {
                        idToTreeName[arr.getJSONObject(j).getInt("masteryId")] = treeName
                    }
                    val fixedObj = JSONObject()
                    fixedObj.put("masteryTreeItems", arr)
                    tree.put(i, fixedObj)
                }
            }
        }

        val data = jsonObject.getJSONObject("data")
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val masteryObj = data.getJSONObject(key)
            masteryObj.put("masteryTree", idToTreeName[Integer.parseInt(key)])
        }

        return jsonObject
    }

    @Throws(IOException::class, JSONException::class)
    private fun makeRequest(request: String, extras: String): String? {
        val urlStr = String.format(URL_TEMPLATE, region, request, extras)
        return HttpClient.makeRequestRaw(urlStr, 0)
    }

    @Throws(IOException::class, JSONException::class)
    fun makeRequestRaw(rawUrl: String, retries: Int): String? {
        return HttpClient.makeRequestRaw(rawUrl, 0)
    }

    @Throws(IOException::class)
    fun getChampionThumb(version: String, championName: String): InputStream {
        val url = URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/champion/%s.png", version, championName))
        return url.openStream()
    }

    @Throws(IOException::class)
    fun getSpellImage(version: String, spellNameWithExtension: String): InputStream {
        val url = URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/spell/%s", version, spellNameWithExtension))
        return url.openStream()
    }

    @Throws(IOException::class)
    fun getSummonerImage(version: String, summonerNameWithExtension: String): InputStream {
        val url = URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/spell/%s", version, summonerNameWithExtension))
        return url.openStream()
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun getPassiveImage(version: String, passiveNameWithExtension: String): InputStream {
        val uri = URI(
                "https",
                "ddragon.leagueoflegends.com",
                String.format("/cdn/%s/img/passive/%s", version, passiveNameWithExtension), null, null)
        val url = URL(uri.toASCIIString())
        return url.openStream()
    }

    @Throws(IOException::class)
    fun getItemImage(version: String, itemId: String): InputStream {
        val url = URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/item/%s.png", version, itemId))
        return url.openStream()
    }

    @Throws(IOException::class)
    fun getPerkImage(version: String, path: String): InputStream {
        val url = URL("https://ddragon.leagueoflegends.com/cdn/img/$path")
        return url.openStream()
    }

    @Throws(IOException::class)
    fun getRuneImage(version: String, runeKey: String): InputStream {
        val url = URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/rune/%s", version, runeKey))
        return url.openStream()
    }

    @Throws(IOException::class)
    fun getMasteryImage(version: String, key: String): InputStream {
        val url = URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/mastery/%s", version, key))
        return url.openStream()
    }

    /**
     * Swaps the "id" and "key" fields as necessary such that the "id" is an int and the "key" is
     * string.
     */
    private fun swapIdWithKey(jsonObject: JSONObject): JSONObject {
        val data = jsonObject.getJSONObject("data")

        val iter = data.keys()
        while (iter.hasNext()) {
            val key = iter.next() as String
            val value = data.getJSONObject(key)

            if (value.get("id") is String) {
                val id = value.getInt("key")
                value.put("key", key)
                value.put("id", id)
            }
        }
        return jsonObject
    }
}
