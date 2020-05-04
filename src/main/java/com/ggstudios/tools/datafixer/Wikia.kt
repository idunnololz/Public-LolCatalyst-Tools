package com.ggstudios.tools.datafixer

import com.ggstudios.tools.datafixer.log.Log
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

object Wikia {
    private val TAG = Wikia::class.java.simpleName

    private val sPoolWorkQueue = LinkedBlockingQueue<Runnable>()
    private val sThreadFactory = object : ThreadFactory {
        private val mCount = AtomicInteger(1)
        override fun newThread(r: Runnable): Thread {
            return Thread(r, "AsyncTask #" + mCount.getAndIncrement())
        }
    }
    private val EXECUTOR = ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() * 2,
            1, TimeUnit.SECONDS,
            sPoolWorkQueue,
            sThreadFactory)

    private val champIdToWikiaData = HashMap<Int, WikiaData>()
    private val VALIDATION_LOCK = Any()

    private const val STATE_NO_DECISION = 0
    private const val STATE_ACCEPT = 1
    private const val STATE_REJECT = 2
    private const val STATE_INVALID = 3

    private val REGEX_COST_BURN = Pattern.compile("([0-9.]+%?\\s*\\/\\s*)*[0-9.]+%?")
    private val EFFECT_PATTERN = Pattern.compile("\\{\\{ ([a-zA-Z0-9]+) \\}\\}")
    private val CD_PATTERN = Pattern.compile("([0-9.]+\\s*\\/\\s*)*[0-9.]+")
    private const val INCREASING = 1
    private const val DECREASING = 2
    private const val NO_CHANGE = 3

    private class SkillData (
        var costBurn: String,
        var costBurnDesc: String,
        var cooldownBurn: String,
        var cooldownBurnDesc: String
    )

    private class WikiaData {
        var skillData: List<SkillData>? = null
        var state = STATE_NO_DECISION
        var attackSpeedOffset: Double = 0.0
    }

    @Throws(IOException::class)
    fun preload(version: String) {
        Log.d(TAG, "### Preloading wikia data... ################################################")

        val championLibrary = StaticDataUtil.makeChampionDictionary(version)

        val futures = ArrayList<Future<*>>()

        for ((_, value) in championLibrary) {
            val future = EXECUTOR.submit {
                try {
                    scrapeWikia(value)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    Log.e(TAG, "Error for champion: ${value.name}")
                    e.printStackTrace()
                }
            }
            futures.add(future)
        }

        for (f in futures) {
            try {
                f.get()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }

        EXECUTOR.shutdown()
        Log.d(TAG, "### Done loading wikia data #################################################")
        Log.d(TAG, "")
    }

    @Throws(IOException::class)
    private fun scrapeWikia(value: StaticDataUtil.ChampionInfo) {
        val url = "http://leagueoflegends.wikia.com/wiki/" + value.name + "/Abilities?action=render"
        val doc = Jsoup.connect(url).get()
        if (doc.select(".skill_q").size == 0) {
            Log.e(TAG, "Error loading page: $url")
            return
        }
        val skillElems = arrayOf(
                doc.select(".skill_q")[0],
                doc.select(".skill_w")[0],
                doc.select(".skill_e")[0],
                doc.select(".skill_r")[0])

        val data = WikiaData()
        var startingState = STATE_NO_DECISION
        val skillData = mutableListOf<SkillData>()
        for (elem in skillElems) {
            val costContainer = elem.select("#costcontainer")
            val cooldownContainer = elem.select("#cooldowncontainer")
            var cost = "0"
            var desc = "Cost not found. Assuming no cost"
            var cd = "0"
            var cdDesc = "Cooldown not found. Assuming no cooldown"
            if (costContainer.size > 0) {
                val costString = costContainer[0].text().replace("\u00a0", " ")
                val matcher = REGEX_COST_BURN.matcher(costString)
                if (matcher.find()) {
                    val found = matcher.group(0)
                    cost = found.replace(" ", "").replace("%", "")

                    // validate costs...
                    val costsStrs = cost.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (costsStrs.size > 1) {
                        val costs = ArrayList<Double>()
                        for (costStr in costsStrs) {
                            costs.add(java.lang.Double.valueOf(costStr))
                        }

                        val trendFollowed = analyzeTrend(costs)
                        if (!trendFollowed) {
                            Log.d(TAG, "State set to invalid since costBurn follows no trend: $costString")
                            Log.d(TAG, "Url $url")
                            startingState = STATE_INVALID
                        }
                    }
                }
                desc = costString
            }
            if (cooldownContainer.size > 0) {
                val cdStr = cooldownContainer.text().replace("\u00a0", " ")
                val matcher = CD_PATTERN.matcher(cdStr)
                if (matcher.find()) {
                    val found = matcher.group(0)
                    cd = found.replace(" ", "")

                    // validate cds...
                    val costsStrs = cd.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (costsStrs.size > 1) {
                        val costs = ArrayList<Double>()
                        for (costStr in costsStrs) {
                            costs.add(java.lang.Double.valueOf(costStr))
                        }

                        val trendFollowed = analyzeTrend(costs)
                        if (!trendFollowed) {
                            Log.d(TAG, "State set to invalid since cdBurn follows no trend: $cdStr")
                            Log.d(TAG, "Url $url")
                            startingState = STATE_INVALID
                        }
                    }
                }
                cdDesc = cdStr
            }
            //Log.d(TAG, cd);

            skillData.add(SkillData(cost, desc, cd, cdDesc))
        }
        data.skillData = skillData
        data.state = startingState

        scrapeWikiaForStaticData(value, data)

        champIdToWikiaData[value.id] = data
    }

    private fun scrapeWikiaForStaticData(value: StaticDataUtil.ChampionInfo, data: WikiaData) {
        // Load full site for other things we need
        val url = "http://leagueoflegends.wikia.com/wiki/${value.name}/Abilities"
        val doc = Jsoup.connect(url).get()

        try {
            val baseAttackSpeed = doc.select("[data-source=\"attack speed\"]").first().ownText().toDouble()
            data.attackSpeedOffset = 0.625 / baseAttackSpeed - 1
        } catch (e: Exception) {
            Log.e(TAG, "Unable to parse attack speed offset for champion ${value.name}. Using default of 0.")
            data.attackSpeedOffset = 0.0
        }
        //Log.d(TAG, "${value.name}: ${data.attackSpeedOffset} [${baseAttackSpeed}]")
    }

    /**
     * Analyzes a list of doubles. Returns true if the list appears to follow a trend. False otherwise.
     *
     * A trend in this case is defined as either always increasing, always decreasing or not changing.
     */
    private fun analyzeTrend(values: List<Double>): Boolean {
        val first = values[0]
        val last = values[values.size - 1]
        val trend = when {
            first > last -> DECREASING
            first == last -> NO_CHANGE
            else -> INCREASING
        }

        var lastVal = java.lang.Double.NaN
        for (value in values) {
            if (java.lang.Double.isNaN(lastVal)) {
                lastVal = value
            } else {
                if (lastVal > value) {
                    if (trend != DECREASING) {
                        return false
                    }
                } else if (lastVal == value) {
                    if (trend != NO_CHANGE) {
                        return false
                    }
                } else {
                    if (trend != INCREASING) {
                        return false
                    }
                }
                lastVal = value
            }
        }
        return true
    }

    fun validate(champDat: JSONObject): JSONObject {
        synchronized(VALIDATION_LOCK) {
            val id = champDat.getInt("id")
            val data = champIdToWikiaData[id]
            if (data == null) {
                Log.d(TAG, "Wikia data for champion not found for: $id")
                return champDat
            }

            champDat.put("attackspeedoffset", data.attackSpeedOffset)

            //champDat = calculateDifferences(champDat, data)

            return champDat
        }
    }

    private fun calculateDifferences(champDat: JSONObject, data: WikiaData): JSONObject {
        val sb = StringBuilder()

        var differences = false
        val old = JSONObject(champDat.toString())
        val spells = champDat.getJSONArray("spells")
        val len = spells.length()
        for (i in 0 until len) {
            val spell = spells.getJSONObject(i)
            val costBurn = spell.optString("costBurn")
            var costAccordingToRiot: String? = ""
            val costRes = spell.optString("resource")
            val cooldownBurn = spell.optString("cooldownBurn")
            //val cooldownAccordingToRiot = spell.optString("cooldownBurn")

            if (costRes == null || costRes.isEmpty()) {
                costAccordingToRiot = costBurn
            } else {
                val matcher = EFFECT_PATTERN.matcher(costRes)
                if (matcher.find()) {
                    val capture = matcher.group(1)
                    costAccordingToRiot = when {
                        capture.startsWith("e") ->
                            spell.getJSONArray("effectBurn").optString(Util.parseFirstInteger(capture))
                        capture == "cost" -> costBurn
                        else -> "0"
                    }
                }
            }

            if (cooldownBurn != data.skillData!![i].cooldownBurn) {
                spell.put("cooldownBurn", data.skillData!![i].cooldownBurn)
                sb.append(String.format(
                        Locale.US,
                        " - CooldownBurn for skill %d is different. %s -> %s\n",
                        i,
                        cooldownBurn,
                        data.skillData!![i].cooldownBurn))
                sb.append("        Desc: ")
                sb.append(data.skillData!![i].cooldownBurnDesc)
                sb.append("\n")
                differences = true
            }

            if (costAccordingToRiot != null) {
                if (costAccordingToRiot == "" && data.skillData!![i].costBurn == "0") {
                    // if riot provided us with an empty cost and the wikia gives us 0, we prefer 0
                    data.state = STATE_ACCEPT
                    continue
                }

                if (costAccordingToRiot != data.skillData!![i].costBurn) {
                    if (!spell.has("costType")) {
                        spell.put("costType", "No Cost")
                    }
                    if (spell.getString("costType") == "No Cost") {
                        // We auto mark a suggestion is invalid if the cost type is no cost.
                        // This is because if Riot explicitly set the cost as no cost then Rito is
                        // prob correct.
                        data.state = STATE_INVALID
                        continue
                    }

                    spell.put("costBurn", data.skillData!![i].costBurn)
                    sb.append(String.format(
                            Locale.US,
                            " - CostBurn for skill %d is different. %s -> %s\n",
                            i,
                            costAccordingToRiot,
                            data.skillData!![i].costBurn))
                    sb.append("        Desc: ")
                    sb.append(data.skillData!![i].costBurnDesc)
                    sb.append("\n")
                    sb.append(String.format(
                            Locale.US,
                            "        RiotData: resource: %s | costType: %s\n",
                            spell.optString("resource").replace("{{ cost }}", costBurn),
                            spell.getString("costType")))
                    differences = true
                }
            }
        }

        if (differences) {
            if (data.state == STATE_NO_DECISION) {
                val url = "http://leagueoflegends.wikia.com/wiki/" + old.getString("name")
                //                    if (old.getString("name").isEmpty()) {
                //                        Log.d(TAG, champDat.toString());
                //                    }
                Log.d("", "")
                Log.d(TAG, "The following differences were detected for champion "
                        + old.getString("name") + "[" + old.getInt("id") + "]")
                val lines = sb.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (l in lines) {
                    Log.d(TAG, l)
                }
                Log.d(TAG, "More info: $url")
                Log.d(TAG, "Would you like to accept these changes? (Y/N)")
                val sc = Scanner(System.`in`)
                var response = sc.nextLine()
                response = response.trim { it <= ' ' }.toLowerCase()
                if (!response.startsWith("y")) {
                    Log.d(TAG, "Change was rejected.")
                    data.state = STATE_REJECT
                } else {
                    Log.d(TAG, "Change was accepted.")
                    data.state = STATE_ACCEPT
                }
            }

            return if (data.state == STATE_ACCEPT) {
                champDat
            } else { // STATE_INVALID, STATE_REJECT
                old
            }
        } else {
            return old
        }
    }
}
