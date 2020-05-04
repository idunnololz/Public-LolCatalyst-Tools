package com.ggstudios.tools.datafixer

import org.json.JSONArray
import org.json.JSONObject

import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

class ChampionInfo {

    private val id: Int = 0
    var name: String? = null
    var title: String? = null
    var key: String? = null
    var lore: String? = null

    var primRole: String? = null
    var secRole: String? = null

    var attack: Int = 0
    var defense: Int = 0
    var magic: Int = 0
    var difficulty: Int = 0

    var partype: Int = 0

    var hp: Double = 0.0
    var hpG: Double = 0.0
    var hpRegen: Double = 0.0
    var hpRegenG: Double = 0.0
    var ms: Double = 0.0
    var mp: Double = 0.0
    var mpG: Double = 0.0
    var mpRegen: Double = 0.0
    var mpRegenG: Double = 0.0
    var range: Double = 0.0
    var ad: Double = 0.0
    var adG: Double = 0.0
    var `as`: Double = 0.0
    var asG: Double = 0.0
    var ar: Double = 0.0
    var arG: Double = 0.0
    var mr: Double = 0.0
    var mrG: Double = 0.0

    private val skillLock = java.lang.Object()
    private var skills: Array<Skill>? = null

    val passive: Passive
        get() = skills!![0] as Passive

    fun setSkills(skills: Array<Skill>) {
        synchronized(skillLock) {
            this.skills = skills
            skillLock.notifyAll()
        }
    }

    fun fullyLoaded() {
        synchronized(skillLock) {
            skillLock.notifyAll()
        }
    }

    fun getSkill(index: Int): Skill {
        return skills!![index + 1]
    }

    class Passive : Skill()

    open class Skill {

        var ranks: Int = 0
        var id: String? = null
        var name: String? = null
        var desc: String? = null
        var details: String? = null
        var varToScaling: Map<String, Scaling> = HashMap()

        var iconAssetName: String? = null

        var rawEffect: JSONArray? = null
        var rawEffectBurn: JSONArray? = null
        var completedDesc: String? = null
        var scaledDesc: String? = null

        var effects: Array<String>? = null

        var defaultKey: String? = null

        var raw: JSONObject? = null

        companion object {
            private val TAG = "Skill"
        }

    }

    class Scaling {
        internal var `var`: String? = null
        internal var coeff: Any? = null
        internal var link: String? = null
    }

    interface OnSkillsLoadedListener {
        fun onSkillsLoaded(skills: Array<Skill>)
    }

    interface OnFullyLoadedListener {
        fun onFullyLoaded()
    }

    companion object {
        val TYPE_UNKNOWN = -1
        val TYPE_MANA = 1
        val TYPE_ENERGY = 2
        val TYPE_BLOODWELL = 3

        private val darkThemeToLightThemeMap = HashMap<String, String>()
        private var pattern: Pattern? = null


        init {
            val patternString = "[A-F0-9]{6}"
            pattern = Pattern.compile(patternString)

            val m = darkThemeToLightThemeMap
            m["0000FF"] = "0000FF"
            m["00DD33"] = "00DD33"
            m["33FF33"] = "33FF33"
            m["44DDFF"] = "44DDFF"
            m["5555FF"] = "5555FF"
            m["6655CC"] = "6655CC"
            m["88FF88"] = "88FF88"
            m["99FF99"] = "99CC00"
            m["CC3300"] = "CC3300"
            m["CCFF99"] = "CCFF99"
            m["DDDD77"] = "DDDD77"
            m["EDDA74"] = "EDDA74"
            m["F50F00"] = "F50F00"
            m["F88017"] = "F88017"
            m["FF0000"] = "FF0000"
            m["FF00FF"] = "FF00FF"
            m["FF3300"] = "FF3300"
            m["FF6633"] = "FF6633"
            m["FF8C00"] = "FF8C00"
            m["FF9900"] = "FF9900"
            m["FF9999"] = "FF9999"
            m["FFAA33"] = "FFAA33"
            m["FFD700"] = "FFD700"
            m["FFDD77"] = "FFDD77"
            m["FFF673"] = "CCC55C"    // light yellow... make it darker yellow
            m["FFFF00"] = "F1C40F"    // yellow illegible on white bg... so use a more orangy yellow
            m["FFFF33"] = "FFFF33"
            m["FFFF99"] = "FFFF99"
            m["FFFFFF"] = "000000"
        }

        fun convertDarkThemeColorToLight(color: String): String {
            return darkThemeToLightThemeMap[color] ?: color
        }

        fun themeHtml(htmlString: String): String {
            val matcher = pattern!!.matcher(htmlString)

            val sb = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(sb, convertDarkThemeColorToLight(matcher.group(0)))
            }
            matcher.appendTail(sb)

            return sb.toString()
        }
    }
}
