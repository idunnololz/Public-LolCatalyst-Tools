package com.ggstudios.tools.datafixer

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class LolUniverse {

    companion object {
        private const val URL_REGIONS = "https://universe-meeps.leagueoflegends.com/v1/en_us/search/index.json"

        private val gson = Gson()
    }

    private data class UniverseChampionDto (
            val type: String,
            @SerializedName("release-date")
            val releaseDate: String,
            val name: String,
            val slug: String,
            @SerializedName("associated-faction-slug")
            val associatedFactionSlug: String
    )

    private data class UniverseSearchDto (
            val id: String,
            val name: String,
            val locale: String,
            val champions: List<UniverseChampionDto>
    )

    private data class FactionInfo (
        val championId: Int,
        val faction: String
    )

    fun generateFactionInfo(version: String) {
        val data = HttpClient.makeRequestRaw(URL_REGIONS, 0)
        val dto = gson.fromJson(data, UniverseSearchDto::class.java)
        val championDic = StaticDataUtil.makeChampionDictionary(version).mapKeys { it.key.toLowerCase() }

        val regionInfo = dto.champions.mapNotNull {
            val id = championDic[it.slug]?.id
            val faction = it.associatedFactionSlug

            id?.let {
                FactionInfo(id, faction)
            } ?: run {
                System.err.println("Unable to find champion $it")
                null
            }
        }

        val file = File("res/raw")
        file.mkdirs()
        val os = DataOutputStream(FileOutputStream("res/raw/factiondata"))
        for ((key, value) in regionInfo) {
            os.writeInt(key)
            os.writeUTF(value)
        }
        os.close()
    }
}
