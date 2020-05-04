package com.ggstudios.tools.datafixer.scrape

import com.ggstudios.tools.datafixer.DataFetcher
import com.ggstudios.tools.datafixer.p
import com.ggstudios.tools.datafixer.pln
import org.json.JSONException
import org.jsoup.Jsoup
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

object SkinsWiki {

    object PriceClass {
        const val CLASSIC_1 = 0
        const val CLASSIC_2 = 1
        const val ROYAL = 2
        const val EPIC = 3
        const val IMPERIAL = 4
        const val LEGENDARY = 5
        const val ULTIMATE = 6
        const val HEXTECH_CRAFTING = 7

        const val UNKNOWN = 200
    }

    private fun priceToPriceClass(price: Int): Int {
        return when (price) {
            10 -> PriceClass.HEXTECH_CRAFTING
            390 -> PriceClass.CLASSIC_1
            520 -> PriceClass.CLASSIC_2
            750 -> PriceClass.ROYAL
            975 -> PriceClass.EPIC
            1350 -> PriceClass.IMPERIAL
            1820 -> PriceClass.LEGENDARY
            3250 -> PriceClass.ULTIMATE
            else -> PriceClass.UNKNOWN
        }
    }

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class)
    fun generateSkinBinary(version: String) {
        // first generate a mapping from skin name to price class
        val executor = DataFetcher.executor
        val priceMap = HashMap<String, Int>()
        // For some reason we are not getting the complete response so we use a raw function to get the html then use Jsoup to parse it
        val src = makeRequestRaw("https://leagueoflegends.fandom.com/wiki/Champion_skin/All_skins", 1)

        val document = Jsoup.parse(src!!)
        val rows = document.select("#mw-content-text table tbody tr")

        //println("[SkinWiki] rows: ${rows.size}")

        if (rows.size < 10) {
            // There is definitely something wrong!
            System.err.println("[SkinWiki] Error parsing site. Only got skin data for ${rows.size} skins")
        }

        for (row in rows) {
            if (row.children().size < 2) continue

            val costCell = row.child(4)
            //pln("costCell: ${costCell.text()}")
            val cost = costCell.text().trim { it <= ' ' }.replace("\u00A0".toRegex(), "")
            if (cost == "Cost") continue // header
            if (cost == "N/A") continue // skin that will never be in the shop (eg. Victorious Elise)
            val skinName = row.child(1).text().trim { it <= ' ' }.replace("\u00A0".toRegex(), "")

            try {
                priceMap[skinName] = priceToPriceClass(Integer.valueOf(cost))
            } catch (e: Exception) {
                System.err.print("Couldn't parse cost for skin [$skinName]. Given: $cost. Cell raw: ${costCell}")
                e.printStackTrace()
            }
        }

        // then load champion json
        val client = DataFetcher.client
        val championJson = client.getAllChampionsFullJsonDd(version).getJSONObject("data")

        val futures = ArrayList<Future<*>>()

        // generate mapping from skin name (lower case) to skin id
        val skinToSkinId = HashMap<String, Int>()
        val iterator = championJson.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()

            futures.add(executor.submit {
                try {
                    val championInfo = client.getChampionJsonDd(version = version, championKey = championJson.getJSONObject(key).getString("key"))

                    synchronized(skinToSkinId) {
                        val skins = championInfo.getJSONArray("skins")
                        for (i in 0 until skins.length()) {
                            val skin = skins.getJSONObject(i)
                            skinToSkinId[skin.optString("name").toLowerCase(Locale.US)] = skin.getInt("id")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        for (future in futures) {
            future.get()
            p("|")
        }

        // finally merge the two maps...
        val skinIdToPrice = HashMap<Int, Int>()
        priceMap.keys.forEach { skinName ->
            val price: Int = priceMap[skinName]!!
            translateSkinAsNeeded(skinName.toLowerCase(Locale.US)).let { translatedSkinName ->
                var id: Int? = skinToSkinId[translatedSkinName.toLowerCase(Locale.US)]
                if (id == null) {
                    var tryName = translatedSkinName
                    while (tryName.isNotEmpty()) {
                        val i = tryName.lastIndexOf(' ')
                        if (i <= 0) break
                        tryName = tryName.substring(0, i)
                        id = skinToSkinId[tryName]
                        if (id != null) break
                    }
                }
                if (id == null) {
                    if (!translatedSkinName.startsWith("original")) {
                        pln("")
                        pln("Error. No such skin in ChampionJson: $translatedSkinName")
                    }
                    return@forEach
                }
                skinIdToPrice[id] = price
            }
        }

        val file = File("res/raw")
        file.mkdirs()
        val os = DataOutputStream(FileOutputStream("res/raw/skindata"))
        for ((key, value) in skinIdToPrice) {
            os.writeInt(key)
            os.writeByte(value)
        }
        os.close()

        pln(" Done")
    }

    @Throws(IOException::class, JSONException::class)
    private fun makeRequestRaw(rawUrl: String, retries: Int): String? {
        val url = URL(rawUrl)

        //System.out.println(url.toString());

        val con = url.openConnection() as HttpURLConnection

        // optional default is GET
        con.requestMethod = "GET"

        val responseCode = con.responseCode
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        when {
            responseCode == HttpURLConnection.HTTP_OK ->
                return BufferedReader(InputStreamReader(con.inputStream)).use { it.readText() }
            responseCode / 100 == 5 -> {
                System.err.println("Server error: $responseCode for url: $url")
                return if (retries <= 2) {
                    // retry...
                    System.err.println("Sleeping for a bit and then retrying...")

                    try {
                        Thread.sleep((1000 * retries).toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    makeRequestRaw(rawUrl, retries + 1)
                } else {
                    System.err.println("Retry limit exceeded... failing...")
                    null
                }
            }
            responseCode == 429 -> { // rate limit hit
                // technically all of the static endpoints we call shouldn't be rate
                val timeToWait = Integer.valueOf(con.headerFields["Retry-After"]?.get(0))

                System.err.println("Rate limit exceeded: $responseCode for url: $url. Waiting: $timeToWait")
                System.err.println(con.headerFields)
                RuntimeException().printStackTrace()


                throw RuntimeException()
            }
            else -> {
                System.err.println("Unexpected response code: $responseCode for url: $url")
                return null
            }
        }
    }

    /**
     * Translates a skin name from league wiki to the official skin name by riot. The arguement is
     * guarenteed to be lower case. The result should be all lower case.
     */
    private fun translateSkinAsNeeded(skinName: String): String {
        when (skinName) {
            "dawnchaser sejuani" -> return "sejuani dawnchaser"
            "project: master yi" -> return "project: yi"
            "project master yi" -> return "project: yi"
            "lightsbane karthus" -> return "karthus lightsbane"
            "pool party dr. mundo" -> return "pool party mundo"
            "brighthammer jayce" -> return "jayce brighthammer"
            "whitebeard ryze" -> return "ryze whitebeard"
            "caskbreaker gragas" -> return "gragas caskbreaker"
            "lionheart braum" -> return "braum lionheart"
            "swiftbolt varus" -> return "varus swiftbolt"
            "captain miss fortune" -> return "captain fortune"
            "project katarina" -> return "project: katarina"
            "project ekko" -> return "project: ekko"
            "project ashe" -> return "project: ashe"
            "project lucian" -> return "project: lucian"
            "project leona" -> return "project: leona"
            "project fiora" -> return "project: fiora"
            "project yasuo" -> return "project: yasuo"
            "project zed" -> return "project: zed"
            "rageborn dr. mundo" -> return "rageborn mundo"
            "el macho dr. mundo" -> return "el macho mundo"
            "corporate dr. mundo" -> return "corporate mundo"
            "status of karthus" -> return "statue of karthus" // typo on wikia
            else -> return skinName
        }
    }
}
