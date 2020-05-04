package com.ggstudios.tools.datafixer

import org.json.JSONObject

import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

object ItemJsonFixer {
    private val itemIdToFix = HashMap<Int, FixData>()

    private val REGEX_PERCENT_STAT = Pattern.compile("(?!</unique>)(<[a-zA-Z/]*>)?[^>]*[ ]*\\+([0-9]+)% *([a-zA-Z ]+)")

    private val REGEX_DEATH_CAP_STATE = Pattern.compile("Increases Ability Power by ([0-9]+)%")

    private val STAT_CDR = "FlatCoolDownRedMod"
    private val STAT_HPR = "FlatHPRegenMod"
    private val STAT_MPR = "FlatMPRegenMod"
    // Ability power percentage mod
    private val STAT_APP = "PercentMagicDamageMod"

    fun fixData(data: JSONObject) {

        val iter = data.keys()
        while (iter.hasNext()) {
            val key = iter.next() as String
            val value = data.getJSONObject(key)

            val itemId = Integer.valueOf(key)
            var fixData: FixData? = itemIdToFix[itemId]
            if (fixData == null) {
                fixData = FixData()

                val desc = value.optString("description")
                var matcher = REGEX_PERCENT_STAT.matcher(desc)
                while (matcher.find()) {
                    val d = java.lang.Double.valueOf(matcher.group(2)) / 100
                    val statName = matcher.group(3).trim { it <= ' ' }

                    when (statName) {
                        "Cooldown Reduction" -> fixData.statsToInject[STAT_CDR] = d
                        "Base Health Regen" -> fixData.statsToInject[STAT_HPR] = d
                        "Base Mana Regen" -> fixData.statsToInject[STAT_MPR] = d
                        "" -> {
                        }
                        else -> println("Unhandled stat name: $statName")
                    }// do nothing
                }

                matcher = REGEX_DEATH_CAP_STATE.matcher(desc)
                if (matcher.find()) {
                    val d = java.lang.Double.valueOf(matcher.group(1)) / 100
                    fixData.statsToInject[STAT_APP] = d
                }

                itemIdToFix[Integer.valueOf(key)] = fixData
            }

            for ((key1, value1) in fixData.statsToInject) {
                value.getJSONObject("stats").put(key1, value1)
            }
        }
    }

    private class FixData {
        val statsToInject = HashMap<String, Double>()
    }
}
