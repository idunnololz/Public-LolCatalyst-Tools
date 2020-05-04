package com.ggstudios.tools.datafixer

import com.ggstudios.tools.datafixer.riot_ddragon.AllChampionsDto
import com.google.gson.Gson

object ChampionLookupTool {

    fun doLookup(input: String) {
        // clean up the input...
        val tokenizedInput: List<String> =
        if (input.contains(",")) {
            input.split(",")
        } else {
            listOf(input)
        }

        val validatedInput = tokenizedInput
                .asSequence()
                .map(String::trim)
                .map(String::toLowerCase)
                .map {
                    it.filter(Char::isLetter)
                }
                .filter(String::isNotBlank)
                .toList()

        val allChampionsJsonDd = LolApiClient.INSTANCE.getAllChampionsJsonDd(DataFetcher.getLatestVersion())
        val data = Gson().fromJson<AllChampionsDto>(allChampionsJsonDd.toString(), AllChampionsDto::class.java)

        val result = validatedInput
                .map { singleInput ->
                    data.data.entries
                            .find {
                                it.key.toLowerCase().startsWith(singleInput)
                            }
                            ?.value
                            ?: run {
                                println("Was unable to find anything close to $singleInput")
                                null
                            }
                }
                .filterNotNull()
        println(result.map {it.name}.joinToString())
        println(result.map {it.id}.joinToString())
    }
}