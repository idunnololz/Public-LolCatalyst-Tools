package com.ggstudios.tools.datafixer

import com.ggstudios.tools.datafixer.riot_ddragon.ReforgedRunePathDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.*
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object DataFetcher {

    private const val LOG_LOCALE_LENGTH = 5
    private const val API_KEY = "RGAPI-879bb32c-a8fb-4dae-865e-b79e5939612a"

    val client: LolApiClient = LolApiClient(LolApiClient.REGION_NA, API_KEY)
    private var latestVersion: String? = null

    private val sPoolWorkQueue = LinkedBlockingQueue<Runnable>()
    val executor: ThreadPoolExecutor

    /**
     * Riot json data has may contain errors so it might actually be beneficial to use an older
     * version of the data. This map is populated with good old versions as needed.
     */
    private val championToBestVersionMap = HashMap<Int, String>()

    private val sThreadFactory = object : ThreadFactory {
        private val mCount = AtomicInteger(1)

        override fun newThread(r: Runnable): Thread {
            return Thread(r, "AsyncTask #" + mCount.getAndIncrement())
        }
    }

    init {
        val cores = Runtime.getRuntime().availableProcessors()
        val baseThreadVal = (cores * 1.5).toInt()
        executor = ThreadPoolExecutor(
                baseThreadVal + 1,
                baseThreadVal * 2,
                1,
                TimeUnit.SECONDS,
                sPoolWorkQueue,
                sThreadFactory)
    }

    @Throws(JSONException::class, IOException::class)
    private fun saveJsonObj(dir: String, obj: JSONObject) {
        val outputStream = FileOutputStream(dir)
        BufferedWriter(OutputStreamWriter(outputStream)).use {
            it.write(obj.toString())
        }
    }

    @Throws(IOException::class, JSONException::class)
    fun listAllVersions() {
        pln(client.versionsDd.toString())
    }

    @Throws(IOException::class)
    fun listAllLanguages() {
        pln(client.languages.toString())
    }

    @Throws(IOException::class, JSONException::class)
    fun getLatestVersion(): String {
        return latestVersion ?: client.versionsDd.getString(0).also {
            latestVersion = it
        }
    }

    /**
     * Get a log friendly string locale given a locale.
     */
    private fun getLogLocale(locale: String?): String {
        return if (locale.orEmpty().length < LOG_LOCALE_LENGTH) {
            String.format("%" + LOG_LOCALE_LENGTH + "s", locale)
        } else {
            locale.orEmpty().substring(0, LOG_LOCALE_LENGTH)
        }
    }

    @Throws(IOException::class, JSONException::class, ExecutionException::class, InterruptedException::class)
    fun fetchAllChampionJson(versionNullable: String?, locale: String?, force: Boolean) {
        val version = versionNullable ?: getLatestVersion()

        var curVer = "NO-VERSION"

        val localePostfix = if (locale == null) "" else "-" + locale.replace("_", "-r")

        val logLocale = getLogLocale(locale)

        val dir = File("res/champions$localePostfix")
        dir.mkdir()
        val rawDir = File("res/raw$localePostfix")
        rawDir.mkdirs()

        val versionFile = File(dir, "version.json")
        try {
            curVer = ChampionInfoFixer.loadJsonObj(versionFile).getString("version")

            if (!force && curVer == version) {
                pln(String.format("[%s] Champion data correct version. No need to re-fetch.", logLocale))
                return
            }
        } catch (e: FileNotFoundException) {
            /* do nothing */
        }

        p(String.format("[%s] Fetching champion data [v%s -> v%s] ", logLocale, curVer, version))

        val futures = LinkedList<Future<*>>()

        val championJson = client.getAllChampionsJsonDd(version, locale)

        // Modify all champion json
        val championsData = championJson.getJSONObject("data")
        val warwick = championsData.getJSONObject("Warwick")
        warwick.put("colloq", "ww")

        run {
            // duplicate the massive champion json object and only extract the fields we need
            val simpleData = JSONObject()
            simpleData.put("version", championJson.getString("version"))

            val data = JSONObject()

            val iter = championsData.keys()
            while (iter.hasNext()) {
                val key = iter.next() as String
                val value = championsData.getJSONObject(key)
                val simpleChampionData = JSONObject()

                simpleChampionData.put("name", value.getString("name"))
                simpleChampionData.put("id", value.getInt("id"))
                simpleChampionData.put("title", value.getString("title"))
                simpleChampionData.put("key", value.getString("key"))

                data.put(key, simpleChampionData)
            }
            simpleData.put("data", data)

            val championJsonFile = File("res/raw$localePostfix/champion.json")
            saveJsonObj(championJsonFile.canonicalPath, simpleData)
        }

        val data = championJson.getJSONObject("data")

        val iter = data.keys()
        while (iter.hasNext()) {
            val key = iter.next() as String
            val value = data.getJSONObject(key)

            futures.add(executor.submit {
                try {
                    val file = File(dir, "$key.json")
                    val champId = value.getInt("id")
                    val versionToUse = championToBestVersionMap[champId]

                    var champDat: JSONObject
                    if (versionToUse == null) {
                        champDat = client.getChampionJsonDd(version, locale, key)
                    } else {
                        champDat = client.getChampionJsonDd(versionToUse, locale, key)

                        val newChampDat = client.getChampionJsonDd(version, locale, key)
                        // copy over skin data...
                        champDat.remove("skins")
                        champDat.put("skins", newChampDat.getJSONArray("skins"))
                    }

                    champDat = Wikia.validate(champDat)
                    champDat = ChampionInfoFixer.fix(champDat)
                    champDat.remove("blurb")
                    champDat.remove("enemytips")
                    champDat.remove("allytips")
                    champDat.remove("recommended")

                    var imageObj = champDat.getJSONObject("image")
                    champDat.remove("image")
                    champDat.put("image", imageObj.getString("full"))

                    // simplify image object for passive
                    val passive = champDat.getJSONObject("passive")
                    imageObj = passive.getJSONObject("image")
                    passive.remove("image")
                    passive.put("image", imageObj.getString("full"))

                    // remove unused fields...
                    passive.remove("sanitizedDescription")

                    val skillsArr = champDat.getJSONArray("spells")
                    val len = skillsArr.length()
                    for (i in 0 until len) {
                        val skillObj = skillsArr.getJSONObject(i)
                        imageObj = skillObj.getJSONObject("image")
                        skillObj.remove("image")
                        skillObj.put("image", imageObj.getString("full"))

                        // remove unused fields...
                        skillObj.remove("sanitizedTooltip")
                        skillObj.remove("sanitizedDescription")
                        skillObj.remove("leveltip")
                    }

                    saveJsonObj(file.canonicalPath, champDat)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    throw e
                }
            })
        }

        for (future in futures) {
            future.get()
            p("|")
        }

        // finally save the version number
        val versionObj = JSONObject()
        versionObj.put("version", version)
        saveJsonObj(versionFile.canonicalPath, versionObj)

        pln(" Done")
    }

    @Throws(IOException::class, JSONException::class, InterruptedException::class, ExecutionException::class)
    fun fetchAllChampionThumb(version: String) {

        fetchThumbs(version,
                { client.getAllChampionsJsonDd(version) },
                { k, _ -> arrayOf(k) },
                { ver, k -> client.getChampionThumb(ver, k) },
                "res/champions_thumb",
                "champion",
                lowerCaseFileNames = true)
    }

    @Throws(IOException::class, JSONException::class)
    fun fetchAllItemInfo(version: String, force: Boolean, locale: String?) {
        fetchInfo(version, { client.getAllItemJsonDd(version, locale) },
                { json ->
                    val data = json.getJSONObject("data")

                    // remove useless top level items
                    json.remove("basic")
                    json.remove("type")
                    json.remove("groups")
                    json.remove("tree")

                    ItemJsonFixer.fixData(data)
                    val keys = data.keys()
                    while (keys.hasNext()) {
                        val itemObj = data.getJSONObject(keys.next())
                        //String imageResName = itemObj.getJSONObject("image").getString("full");
                        itemObj.remove("image")
                        //itemObj.put("image", imageResName);
                        itemObj.remove("plaintext")
                        itemObj.remove("effect")

                        val gold = itemObj.optJSONObject("gold")
                        if (gold.optInt("total", 0) == 0) {
                            gold.remove("total")
                        }
                        if (gold.optInt("sell", 0) == 0) {
                            gold.remove("sell")
                        }
                        if (gold.optInt("base", 0) == 0) {
                            gold.remove("base")
                        }

                        //val tags = itemObj.optJSONArray("tags")

                        val colloq = itemObj.optString("colloq", null)
                        if (colloq == null || colloq.isEmpty() || colloq == ";") {
                            itemObj.remove("colloq")
                        }
                    }
                    json
                },
                "item.json", "items", force, locale)
    }

    @Throws(IOException::class, JSONException::class)
    fun fetchAllRuneInfo(version: String, force: Boolean, locale: String) {
        fetchInfo(version, { client.getAllRuneJsonDd(version, locale) },
                { json ->
                    val data = json.getJSONObject("data")
                    val keys = data.keys()
                    while (keys.hasNext()) {
                        val itemObj = data.getJSONObject(keys.next())
                        val imageResName = itemObj.getJSONObject("image").getString("full")
                        itemObj.remove("image")
                        itemObj.put("image", imageResName)
                    }
                    json
                }, "rune.json", "runes", force, locale)
    }

    @Throws(IOException::class)
    fun fetchAllSummonerInfo(version: String, force: Boolean, locale: String?) {
        fetchInfo(version, { client.getAllSummonerJsonDd(version, locale) }, { json ->
            val data = json.getJSONObject("data")
            val keys = data.keys()
            while (keys.hasNext()) {
                val itemObj = data.getJSONObject(keys.next())
                val imageResName = itemObj.getJSONObject("image").getString("full")
                itemObj.remove("image")
                itemObj.put("image", imageResName)
            }
            json
        }, "summoner.json", "summoner spells", force, locale)
    }

    @Throws(IOException::class)
    fun fetchAllPerksInfo(version: String, force: Boolean, locale: String?, fileName: String? = "perks.json"): JSONObject? {
        return fetchInfo(version, { client.getAllPerksJsonDd(version, locale) }, { json ->
            //            JSONObject result = json.getJSONObject("result");
            //            Iterator<String> keys = result.keys();
            //            while (keys.hasNext()) {
            //                JSONObject itemObj = result.getJSONObject(keys.next());
            //                String imageResName = itemObj.getJSONObject("image").getString("full");
            //                itemObj.remove("image");
            //                itemObj.put("image", imageResName);
            //            }

            val result = JSONObject()

            val paths = Gson().fromJson<List<ReforgedRunePathDto>>(json.getJSONArray("data").toString(),
                    object : TypeToken<List<ReforgedRunePathDto>>() {}.type)

            val data = JSONObject()
            val trees = JSONObject()
            for (path in paths) {
                val tree = JSONArray()

                run {
                    val row = JSONArray()
                    row.put(path.id)
                    tree.put(row)

                    val rune = JSONObject()
                    rune.put("name", path.name)
                    rune.put("desc", path.name)
                    if (locale == null) {
                        rune.put("iconPath", path.icon)
                    }
                    data.put(path.id.toString(), rune)
                }

                for (slotDto in path.slots!!) {
                    val row = JSONArray()

                    for (runeDto in slotDto.runes!!) {
                        row.put(runeDto.id)

                        val rune = JSONObject()
                        rune.put("name", runeDto.name)
                        rune.put("desc", runeDto.longDesc)
                        if (locale == null) {
                            rune.put("iconPath", runeDto.icon)
                        }
                        data.put(runeDto.id.toString(), rune)
                    }
                    tree.put(row)
                }
                trees.put(path.id.toString(), tree)
            }
            result.put("trees", trees)
            result.put("data", data)

            // make treeNodes
            val treeNodesArr = JSONArray()
            treeNodesArr.put(8000)
            treeNodesArr.put(8100)
            treeNodesArr.put(8200)
            treeNodesArr.put(8400)
            treeNodesArr.put(8300)
            result.put("treeNodes", treeNodesArr)

            PerkInfoFixer.fix(result)

            //pln(result.toString());
            result
        }, fileName, "perks", force, locale)
    }

    @Throws(IOException::class)
    fun fetchAllMasteryInfo(version: String, force: Boolean, locale: String) {
        fetchInfo(version, { client.getAllMasteryJsonDd(version, locale) }, { json ->
            val data = json.getJSONObject("data")
            val keys = data.keys()
            while (keys.hasNext()) {
                val itemObj = data.getJSONObject(keys.next())
                val imageResName = itemObj.getJSONObject("image").getString("full")
                itemObj.remove("image")
                itemObj.put("image", imageResName)
            }
            json
        }, "mastery.json", "masteries", force, locale)
    }

    @Throws(IOException::class, JSONException::class, InterruptedException::class, ExecutionException::class)
    fun fetchAllSummonerThumb(version: String) {
        fetchThumbs(version,
                { client.getAllSummonerJsonDd(version, null) },
                { _, v ->
                    val key = v.getJSONObject("image").getString("full")
                    arrayOf(key)
                },
                { ver, k -> client.getSummonerImage(ver, k) },
                "res/summoner",
                "summoner")
    }

    @Throws(IOException::class, JSONException::class, InterruptedException::class, ExecutionException::class)
    fun fetchAllSpellThumb(version: String) {
        fetchThumbs(version,
                { client.getAllChampionsFullJsonDd(version) },
                { _, v ->
                    val spells = v.getJSONArray("spells")

                    Array(spells.length()) {
                        val spell = spells.getJSONObject(it)
                        val image = spell.getJSONObject("image")

                        image.getString("full")
                    }
                },
                { ver, k -> client.getSpellImage(ver, k) },
                "res/spells",
                "spell",
                0.25f)
    }

    @Throws(IOException::class, JSONException::class, URISyntaxException::class, ExecutionException::class, InterruptedException::class)
    fun fetchAllPassiveThumb(version: String) {
        fetchThumbs(version,
                { client.getAllChampionsFullJsonDd(version) },
                { _, v ->
                    val passive = v.getJSONObject("passive")
                    val image = passive.getJSONObject("image")
                    val imageName = image.getString("full")
                    arrayOf(imageName)
                },
                { ver, k -> client.getPassiveImage(ver, k) },
                "res/passive",
                "passive",
                lowerCaseFileNames = true)
    }

    @Throws(IOException::class, JSONException::class, URISyntaxException::class, ExecutionException::class, InterruptedException::class)
    fun fetchAllItemThumb(version: String) {
        fetchThumbs(version,
                { client.getAllItemJsonDd(version) },
                { k, _ -> arrayOf(k) },
                { ver, k -> client.getItemImage(ver, k) },
                "res/item_thumb",
                "item",
                0.5f)
    }

    fun fetchAllPerksThumb(version: String) {
        fetchThumbs(
                version,
                { checkNotNull(fetchAllPerksInfo(version, true,null, fileName = null)) },
                { k, json ->
                    arrayOf(json.getString("iconPath")) },
                { ver, k -> client.getPerkImage(ver, k) },
                "res/perks",
                "perk",
                0.5f,
                getFileNameFn = { k, iconKey, json ->
                    "$k.png"
                })
    }

    @Throws(IOException::class, JSONException::class)
    private fun fetchInfo(
            versionNullable: String?,
            fetchDataFn: () -> JSONObject,
            postProcessingFn: ((input: JSONObject) -> JSONObject)?,
            outputFile: String?,
            desc: String,
            force: Boolean,
            locale: String?
    ): JSONObject? {
        val version = versionNullable ?: getLatestVersion()

        val d: String = if (locale == null) {
            "res/raw"
        } else {
            "res/raw-" + locale.replace("_", "-r")
        }
        val dir = File(d)
        dir.mkdirs()
        var json = fetchDataFn()

        val file = if (outputFile == null) null else File(dir, outputFile)

        if (file != null) {
            var curVer = "0.0.0"
            try {
                curVer = ChampionInfoFixer.loadJsonObj(file).getString("version")
            } catch (e: Exception) { /* do nothing */ }

            if (curVer == version && !force) {
                pln(String.format("[%s] Data for '$desc' correct version. No need to re-fetch.", getLogLocale(locale)))
                return null
            }

            p(String.format("[%s] Retrieving $desc data (v%s -> v%s) |", getLogLocale(locale), curVer, version))
        }

        if (postProcessingFn != null) {
            json = postProcessingFn(json)
        }

        if (file != null) {
            saveJsonObj(file.canonicalPath, json)
        }

        pln("Done")

        return json
    }

    @Throws(IOException::class, JSONException::class, InterruptedException::class, ExecutionException::class)
    private fun fetchThumbs(
            versionNullable: String?,
            fetchDataFn: () -> JSONObject,
            fetchThumbKeyFn: (key: String, o: JSONObject) -> Array<String>,
            fetchThumbFn: (version: String, key: String) -> InputStream,
            dirName: String,
            desc: String,
            progressPerTask: Float = 1f,
            lowerCaseFileNames: Boolean = false,
            getFileNameFn: ((key: String, iconKey: String, o: JSONObject) -> String)? = null) {
        val version = versionNullable ?: getLatestVersion()

        p("Fetching all $desc thumbnails ")

        val dir = File(dirName)
        dir.mkdir()

        val json = fetchDataFn()
        val data = json.getJSONObject("data")

        p(String.format("(v%s) ", version))

        val futures = LinkedList<Future<*>>()

        val iter = data.keys()
        while (iter.hasNext()) {
            val k = iter.next() as String
            val key = fetchThumbKeyFn(k, data.getJSONObject(k))
            for (s in key) {
                futures.add(executor.submit {
                    val fileName: String = if (getFileNameFn != null) {
                        getFileNameFn(k, s, data.getJSONObject(k))
                    } else if (s.indexOf('.') == -1) {
                        "${s}.png"
                    } else {
                        s
                    }.let {
                        if (lowerCaseFileNames) {
                            it.toLowerCase(Locale.US)
                        } else it
                    }

                    try {
                        val os = FileOutputStream(dirName + File.separator + fileName)

                        fetchThumbFn(version, s).use {
                            val b = ByteArray(2048)
                            var length: Int

                            while (it.read(b).also { length = it } != -1) {
                                os.write(b, 0, length)
                            }
                        }
                        os.close()
                    } catch (e: IOException) {
                        System.err.println("\nError fetching thumbnail: $fileName")
                        e.printStackTrace()
                    } catch (e: URISyntaxException) {
                        System.err.println("\nError fetching thumbnail: $fileName")
                        e.printStackTrace()
                    }
                })
            }
        }

        var p = 0f
        for (future in futures) {
            future.get()
            p += progressPerTask
            if (p >= 1) {
                p = 0f
                p("|")
            }
        }
        pln(" Done")
    }

    fun end() {
        executor.shutdown()
    }

    @Throws(IOException::class)
    fun listBestVersions() {
        val versions = client.versionsDd
        val len = versions.length()
        val allVersions = ArrayList<String>(len)
        for (i in 0 until len) {
            allVersions.add(versions.getString(i))
        }

        val futures = LinkedList<Future<*>>()

        val championJson = client.getAllChampionsJsonDd(allVersions[0])

        val data = championJson.getJSONObject("data")

        val iter = data.keys()
        while (iter.hasNext()) {
            val key = iter.next() as String

            futures.add(executor.submit {
                var versionIndex = 0
                try {
                    outer@ while (true) {
                        val version = allVersions[versionIndex]
                        val champDat = client.getChampionJsonDd(version = version, championKey = key)
                        val spells = champDat.getJSONArray("spells")
                        val spellsCount = spells.length()
                        for (i in 0 until spellsCount) {
                            val spell = spells.getJSONObject(i)
                            val tooltip = spell.optString("tooltip")
                            val sanitizedTooltip = Jsoup.parse(tooltip).text().trim { it <= ' ' }
                            if (sanitizedTooltip.length <= 3) {
                                versionIndex++
                                continue@outer
                            }
                        }

                        if (versionIndex > 0) {
                            championToBestVersionMap[champDat.getInt("id")] = allVersions[versionIndex]
                        }

                        break
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            })
        }

        for (future in futures) {
            try {
                future.get()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }

            p("|")
        }

        pln("")
        pln("Done scan. Best versions: " + Arrays.asList<Map<Int, String>>(championToBestVersionMap).toString())
    }
}
