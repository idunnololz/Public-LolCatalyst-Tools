package com.ggstudios.tools.datafixer

import com.ggstudios.tools.datafixer.bin.BinMain
import com.ggstudios.tools.datafixer.bin.TftDataGenerator
import com.ggstudios.tools.datafixer.bin.WadFilesManager
import com.ggstudios.tools.datafixer.scrape.SkinsWiki

import org.apache.commons.io.FileUtils
import org.json.JSONException

import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.concurrent.ExecutionException
import kotlin.math.abs

object Main {

    private fun doCopy() {
        val source = File(Util.BASE_DIR + "raw")
        val dest = File(Util.BASE_APP_DIR + "raw")
        try {
            FileUtils.copyDirectory(source, dest)
            pln("Copied " + source.absolutePath + " to " + dest.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class, JSONException::class)
    private fun fetchInfo(version: String, force: Boolean, locales: Array<String?>) {
        for (l in locales) {
            DataFetcher.fetchAllChampionJson(version, l, force)
            DataFetcher.fetchAllSummonerInfo(version, force, l)
            DataFetcher.fetchAllItemInfo(version, force, l)
            DataFetcher.fetchAllPerksInfo(version, force, l)
            //            DataFetcher.fetchAllRuneInfo(version, force, l);
            //            DataFetcher.fetchAllMasteryInfo(version, force, l);
        }
    }

    @Throws(IOException::class, JSONException::class, URISyntaxException::class, InterruptedException::class, ExecutionException::class)
    private fun fetchAll(version: String, force: Boolean) {
        val dir = File("res/champions")
        dir.mkdirs()

        fetchInfo(version, force, arrayOf(
                null,
                "cs_CZ",
                "de_DE",
                "es_MX",
                "es_AR",
                "es_ES",
                "fr_FR",
                "el_GR",
                "hu_HU",
                "it_IT",
                "ja_JP",
                "ko_KR",
                "pl_PL",
                "pt_BR",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "zh_CN",
                "zh_TW"))

        DataFetcher.fetchAllChampionThumb(version)
        DataFetcher.fetchAllSpellThumb(version)
        DataFetcher.fetchAllPassiveThumb(version)
        DataFetcher.fetchAllSummonerThumb(version)
        DataFetcher.fetchAllItemThumb(version)
        DataFetcher.fetchAllPerksThumb(version)
        //DataFetcher.fetchAllRuneThumb(version);
        //DataFetcher.fetchAllMasteryThumb(version);

        //ChampionInfoFixer.fixChampionInfo();
    }

    private fun downloadAllData(versionString: String) {
        Wikia.preload(versionString)

        // necessary to correct data issues that might not be true for earlier data versions
        DataFetcher.listBestVersions()

        fetchAll(versionString, true /* force */)

        SkinsWiki.generateSkinBinary(versionString)
        LolUniverse().generateFactionInfo(versionString)

        DataFetcher.end()

        // auto copy script will copy everything in the raw folder to the raw directory of the app
        doCopy()

        // patch resources...
        Patch.doPatch()

        pln("Done!")
    }

    @Throws(IOException::class, InterruptedException::class, ExecutionException::class, URISyntaxException::class)
    private fun doConsole(versionString: String) {

        println("LoLCatalyst Tool")
        println("----------------")
        //DataFetcher.fetchAllPerksInfo(versionString, true, null)

        //SkinsWiki.generateSkinBinary(versionString)

        Console.newBuilder().apply {
            addOption("List all versions") { DataFetcher.listAllVersions() }
            addOption("List all languages") { DataFetcher.listAllLanguages() }
            addOption(String.format("Download all data [%s]", versionString)) {
                downloadAllData(versionString)

                LolUniverse().generateFactionInfo(versionString)

                it.exitMenu()
            }
            addOption(String.format("Download [EN] data [%s]", versionString)) {
                Wikia.preload(versionString)

                // necessary to correct data issues that might not be true for earlier data versions
                DataFetcher.listBestVersions()

                fetchInfo(versionString, true, arrayOf("en_US"))

                SkinsWiki.generateSkinBinary(versionString)

                DataFetcher.end()

                // auto copy script will copy everything in the raw folder to the raw directory of the app
                doCopy()

                // patch resources...
                Patch.doPatch()

                LolUniverse().generateFactionInfo(versionString)

                it.exitMenu()
            }
            addOption("Lookup champion id(s)") {
                println("Please enter champion name(s), comma separated please:")
                ChampionLookupTool.doLookup(it.readNextLine())
                it.exitMenu()
            }
            addOption("Fetch region data") {
                LolUniverse().generateFactionInfo(versionString)
                it.exitMenu()
            }
            addOption("Generate TFT data") {
                TftDataGenerator.generate(WadFilesManager())
                doCopy()
                it.exitMenu()
            }
            addOption("Generate TFT data (with clear cache)") {
                TftDataGenerator.generate(WadFilesManager(), clearCache = true)
                doCopy()
                it.exitMenu()
            }
            addOption("Bin menu", "bin") {
                BinMain.start()
                it.exitMenu()
            }
            addOption("Exit", "exit,q") { it.exitMenu() }
        }.build().show()


        //DataFetcher.fetchAllPerksInfo(VERSION_STRING, true, null);
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val versionString = "10.9.1"

        //RandLol().dothing()

        //println(byte.and(0xFF))

        // Download all ddragon data
        //downloadAllData(versionString)


        //fetchInfo(versionString, true, arrayOf("zh_CN"))

        // Generate tft data from game files
        //TftDataGenerator.generate(WadFilesManager())
        //TftDataGenerator.generate(WadFilesManager(), clearCache = true)

        try {
            doConsole(versionString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //Ctf.do4()
    }
}
