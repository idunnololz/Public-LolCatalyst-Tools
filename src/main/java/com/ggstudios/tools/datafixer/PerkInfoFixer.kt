package com.ggstudios.tools.datafixer

import com.ggstudios.tools.datafixer.log.Log

import org.json.JSONObject

import java.util.HashMap
import java.util.regex.Pattern

object PerkInfoFixer {
    private val TAG = PerkInfoFixer::class.java.simpleName

    private val SPECIAL_VARIABLE_PATTERN = Pattern.compile("@([a-zA-Z0-9.*-\\\\ ]+?)@")

    private val fixDict = HashMap<String, HashMap<String, Any>>()

    private fun addFix(perkId: Int, key: String, value: Any) {
        val perkIdStr = perkId.toString()

        val perkKeyDict = fixDict.computeIfAbsent(perkIdStr) { HashMap() }

        val previous = perkKeyDict.put(key, value)
        if (previous != null) {
            throw RuntimeException("More than one fix added for $perkId, $key")
        }
    }

    private fun makeFixDict() {
        if (!fixDict.isEmpty()) {
            return
        }

        addFix(8134, "StartingActiveItemCDR.0*100", 10) // Ingenious Hunter
        addFix(8134, "ActiveItemCDRPerStack.0*100", 6) // Ingenious Hunter

        addFix(8299, "MinBonusDamagePercent.0*100", 5) // Last Stand
        addFix(8299, "MaxBonusDamagePercent.0*100", 11) // Last Stand
        addFix(8299, "HealthThresholdStart.0*100", 60) // Last Stand
        addFix(8299, "HealthThresholdEnd.0*100", 30) // Last Stand

        addFix(8453, "StandardAmp.0", 5) // Revitalize
        addFix(8453, "ExtraAmp.0", 10) // Revitalize
        addFix(8453, "HealthCutoff.0", 40) // Revitalize

        addFix(8135, "StartingOmnivamp*100", 2.5) // Ravenous Hunter
        addFix(8135, "OmnivampPerStack*100", 2.5) // Ravenous Hunter

        addFix(8410, "MovementSpeedPercentBonus.0*100", 15) // Approach Velocity
        addFix(8410, "ActivationDistance", 1000) // Approach Velocity

        addFix(9103, "LifeStealPerStack*100", 0.8) // Legend: Bloodline
        addFix(9103, "MaxLegendStacks", 10) // Legend: Bloodline

        addFix(8014, "BonusPercentDamage.0 *100", 7) // Coup de Grace
        addFix(8014, "EnemyHealthPercentageThreshold*100", 40) // Coup de Grace
        addFix(8014, "AdaptiveForce.-1*0.6", 9) // Coup de Grace
        addFix(8014, "AdaptiveForce", 15) // Coup de Grace
        addFix(8014, "Duration", 10) // Coup de Grace

        addFix(8451, "MaxHealthRatioPerTier*100", .2) // Overgrowth
        addFix(8451, "UnitsPerTier", 8) // Overgrowth

        addFix(9101, "ShieldCapRatio.0*100", 10) // Overheal
        addFix(9101, "MaxBaseShieldCap", 10) // Overheal
        addFix(9101, "ShieldGenerationRateSelf.0*100", 40) // Overheal
        addFix(9101, "ShieldGenerationRateOtherMax.0*100", 300) // Overheal

        addFix(8210, "MaxCDR*100", 10) // Transcendence
        addFix(8210, "LevelToTurnOn", 10) // Transcendence
        addFix(8210, "AdaptiveForce.-1*0.6", 1.2) // Transcendence
        addFix(8210, "AdaptiveForce", 2) // Transcendence

        addFix(8138, "AdaptiveForce.-1*0.6", 0.6) // Eyeball Collection
        addFix(8138, "AdaptiveForce", 1) // Eyeball Collection
        addFix(8138, "MaxEyeballs", 20) // Eyeball Collection
        addFix(8138, "CompletionBonus.-1*0.6", 6) // Eyeball Collection
        addFix(8138, "CompletionBonus", 10) // Eyeball Collection
        addFix(8138, "StacksPerTakedown", 2) // Eyeball Collection
        addFix(8138, "StacksPerAssist", 1) // Eyeball Collection
        addFix(8138, "StacksPerWard", 1) // Eyeball Collection

        addFix(8017, "MinBonusDamagePercent.0*100", 4) // Cut Down
        addFix(8017, "MinHealthDifference", 150) // Cut Down
        addFix(8017, "MaxBonusDamagePercent.0*100", 12) // Cut Down
        addFix(8017, "MaxHealthDifference", 2000) // Cut Down

        addFix(8139, "HealAmount", 18) // Taste of Blood
        addFix(8139, "HealAmountMax", 35) // Taste of Blood
        addFix(8139, "ADRatio.-1", .2) // Taste of Blood
        addFix(8139, "APRatio.-1", .1) // Taste of Blood
        addFix(8139, "Cooldown", 20) // Taste of Blood

        addFix(8136, "WardDurationTooltipMin", 30) // Zombie Ward
        addFix(8136, "WardDurationMin", 36) // Zombie Ward
        addFix(8136, "WardDuration", 120) // Zombie Ward

        addFix(9104, "AttackSpeedBase*100", 3) // Legend: Alacrity
        addFix(9104, "AttackSpeedPerStack*100", 1.5) // Legend: Alacrity
        addFix(9104, "MaxLegendStacks", 10) // Legend: Alacrity

        addFix(9105, "TenacityBase*100", 5) // Legend: Tenacity
        addFix(9105, "TenacityPerStack*100", 2.5) // Legend: Tenacity
        addFix(9105, "MaxLegendStacks", 10) // Legend: Tenacity

        addFix(8214, "DamageBase", 15) // Summon Aery
        addFix(8214, "DamageMax", 40) // Summon Aery
        addFix(8214, "DamageAPRatio.-1", 0.1) // Summon Aery
        addFix(8214, "DamageADRatio.-1", 0.15) // Summon Aery
        addFix(8214, "ShieldBase", 30) // Summon Aery
        addFix(8214, "ShieldMax", 80) // Summon Aery
        addFix(8214, "ShieldRatio.-1", 0.25) // Summon Aery
        addFix(8214, "ShieldRatioAD.-1", 0.4) // Summon Aery

        addFix(8010, "ThresholdTime", 4) // Conqueror
        addFix(8010, "BaseAD", 10) // Conqueror
        addFix(8010, "MaxAD", 35) // Conqueror
        addFix(8010, "BuffDuration", 3) // Conqueror
        addFix(8010, "TrueDamageBase*100", 20) // Conqueror

        addFix(8008, "LeadInDelay.1", 1.5) // Lethal Tempo
        addFix(8008, "AttackSpeedMin*100", 30) // Lethal Tempo
        addFix(8008, "AttackSpeedMax*100", 80) // Lethal Tempo
        addFix(8008, "AttackSpeedBuffDurationMin", 3) // Lethal Tempo
        addFix(8008, "AttackSpeedBuffDurationMax", 6) // Lethal Tempo
        addFix(8008, "Cooldown", 6) // Lethal Tempo

        addFix(8009, "PercentManaRestore*100", 20) // Presence of Mind
        addFix(8009, "UltimateCooldownRefund*100", 10) // Presence of Mind

        addFix(8465, "SnuggleRange", 175) // Guardian
        addFix(8465, "GuardDuration", 2.5) // Guardian
        addFix(8465, "ShieldDuration", 1.5) // Guardian
        addFix(8465, "Cooldown", 70) // Guardian
        addFix(8465, "CooldownMaxLevel", 40) // Guardian
        addFix(8465, "ShieldBase", 70) // Guardian
        addFix(8465, "ShieldMax", 150) // Guardian
        addFix(8465, "APRatio.-1", 0.25) // Guardian
        addFix(8465, "HPRatio.0*100", 12) // Guardian
        addFix(8465, "Haste*100", 20) // Guardian

        addFix(8143, "BonusLethality.0", 10) // Sudden Impact
        addFix(8143, "BonusMpen.0", 8) // Sudden Impact
        addFix(8143, "Duration", 5) // Sudden Impact
        addFix(8143, "Cooldown", 4) // Sudden Impact

        addFix(9111, "MissingHealthRestored.0*100", 12) // Triumph
        addFix(9111, "BonusGold", 20) // Triumph

        addFix(8463, "MarkDuration", 5) // Font of Life
        addFix(8463, "FlatHealAmount", 5) // Font of Life
        addFix(8463, "HealthRatio.-1 * 100", 1) // Font of Life
        addFix(8463, "HealDuration", 2) // Font of Life

        addFix(8105, "StartingOOCMS", 8) // Relentless Hunter
        addFix(8105, "OOCMS.0", 8) // Relentless Hunter

        addFix(8347, "CDR*100", 5) // Cosmic Insight

        addFix(8226, "ManaIncrease", 25) // Manaflow Band
        addFix(8226, "MaxManaIncrease", 250) // Manaflow Band
        addFix(8226, "PercentManaRestore*100", 1) // Manaflow Band
        addFix(8226, "PercentManaRestoreCooldown", 5) // Manaflow Band
        addFix(8226, "Cooldown.0", 15) // Manaflow Band

        addFix(8304, "GiveBootsAtMinute", 10) // Magical Footwear
        addFix(8304, "SecondsSoonerPerTakedown", 30) // Magical Footwear
        addFix(8304, "AdditionalMovementSpeed", 10) // Magical Footwear

        addFix(8345, "BiscuitMinuteInterval", 3) // Biscuit Delivery
        addFix(8345, "SwapOverMinute", 12) // Biscuit Delivery
        addFix(8345, "HealthHealPercent.0*100", 15) // Biscuit Delivery
        addFix(8345, "PermanentMana", 40) // Biscuit Delivery
        addFix(8345, "HealthHealPercentManaless.0*100", 20) // Biscuit Delivery

        addFix(8224, "PercHealthTrigger.0*100", 30) // Nullifying Orb
        addFix(8224, "ShieldMin", 40) // Nullifying Orb
        addFix(8224, "ShieldMax", 120) // Nullifying Orb
        addFix(8224, "APRatio.-1", .1) // Nullifying Orb
        addFix(8224, "ADRatio.-1", .15) // Nullifying Orb
        addFix(8224, "ShieldDuration", 4) // Nullifying Orb
        addFix(8224, "Cooldown", 60) // Nullifying Orb

        addFix(8021, "HealBase", 3) // Fleet Footwork
        addFix(8021, "HealMax", 60) // Fleet Footwork
        addFix(8021, "HealBonusADRatio.-1", .3) // Fleet Footwork
        addFix(8021, "HealAPRatio.-1", .4) // Fleet Footwork
        addFix(8021, "MSBuff*100", 30) // Fleet Footwork
        addFix(8021, "MSDuration.0", 1) // Fleet Footwork
        addFix(8021, "MinionHealMod*100", 60) // Fleet Footwork
        addFix(8021, "MinionHealMod*50", 30) // Fleet Footwork
        addFix(8021, "HealCritMod*100", 40) // Fleet Footwork

        addFix(8112, "WindowDuration", 3) // Electrocute
        addFix(8112, "DamageBase", 50) // Electrocute
        addFix(8112, "DamageMax", 220) // Electrocute
        addFix(8112, "BonusADRatio.-1", 0.5) // Electrocute
        addFix(8112, "APRatio.-1", 0.3) // Electrocute
        addFix(8112, "Cooldown", 50) // Electrocute
        addFix(8112, "CooldownMin", 25) // Electrocute

        addFix(8233, "MinAdaptive.-1*0.6", 3) // Absolute Focus
        addFix(8233, "MinAdaptive", 5) // Absolute Focus
        addFix(8233, "HealthPercent*100", 70) // Absolute Focus
        addFix(8233, "MaxAdaptive.-1*0.6", 24) // Absolute Focus
        addFix(8233, "MaxAdaptive", 40) // Absolute Focus

        addFix(8234, "PercentMS.0", 3) // Celerity
        addFix(8234, "AdaptiveAD*8", 4.8) // Celerity
        addFix(8234, "AdaptiveAP*8", 8) // Celerity

        addFix(8352, "PotionDuration*100", 20) // Time Warp Tonic
        addFix(8352, "BonusMS*100", 5) // Time Warp Tonic

        addFix(8473, "BlockCount", 3) // Bone Plating
        addFix(8473, "BlockBase", 20) // Bone Plating
        addFix(8473, "BlockMax", 50) // Bone Plating
        addFix(8473, "BlockDuration", 3) // Bone Plating
        addFix(8473, "Cooldown", 45) // Bone Plating

        addFix(8232, "MovementSpeed", 25) // Waterwalking
        addFix(8232, "MaxAdaptive.-1*0.6", 18) // Waterwalking
        addFix(8232, "MaxAdaptive", 30) // Waterwalking

        addFix(8237, "damage", 20) // Scorch
        addFix(8237, "damagemax", 60) // Scorch
        addFix(8237, "dotduration", 1) // Scorch
        addFix(8237, "BurnlockoutDuration", 20) // Scorch

        addFix(8313, "InitialCooldown.0", 10) // Perfect Timing
        addFix(8313, "PercentGAZhonyasCDR.0*100", 15) // Perfect Timing

        addFix(8236, "UpdateAfterMinutes", 10) // Gathering Storm

        addFix(8351, "SlowDuration.0", 2) // Glacial Augment
        addFix(8351, "SlowAmountBase.0*-100", 30) // Glacial Augment
        addFix(8351, "SlowAmountMax.0*-100", 40) // Glacial Augment
        addFix(8351, "SlowAmountBaseMelee.0*-100", 45) // Glacial Augment
        addFix(8351, "SlowAmountMaxMelee.0*-100", 55) // Glacial Augment
        addFix(8351, "SlowZoneDuration", 5) // Glacial Augment
        addFix(8351, "SlowZoneSlow*-100", 60) // Glacial Augment
        addFix(8351, "UnitCDBase", 7) // Glacial Augment
        addFix(8351, "UnitCD16", 4) // Glacial Augment

        addFix(8472, "StartingHealth", 60) // Chrysalis
        addFix(8472, "MaxTakedowns", 4) // Chrysalis
        addFix(8472, "AdaptiveForce.-1*0.6", 9) // Chrysalis
        addFix(8472, "AdaptiveForce", 15) // Chrysalis

        addFix(8230, "Window", 3) // Phase Rush
        addFix(8230, "HasteBase*100", 15) // Phase Rush
        addFix(8230, "HasteMax*100", 40) // Phase Rush
        addFix(8230, "SlowResist*100", 75) // Phase Rush
        addFix(8230, "Duration", 3) // Phase Rush
        addFix(8230, "Cooldown", 15) // Phase Rush

        addFix(8429, "MinutesRequired", 10) // Conditioning
        addFix(8429, "ArmorBase", 8) // Conditioning
        addFix(8429, "MRBase", 8) // Conditioning
        addFix(8429, "ExtraResist", 5) // Conditioning

        addFix(8306, "ChannelDuration", 2) // Hextech Flashtraption
        addFix(8306, "CooldownTime", 20) // Hextech Flashtraption
        addFix(8306, "ChampionCombatCooldown", 10) // Hextech Flashtraption

        addFix(8229, "DamageBase", 30) // Arcane Comet
        addFix(8229, "DamageMax", 100) // Arcane Comet
        addFix(8229, "APRatio.-1", .2) // Arcane Comet
        addFix(8229, "ADRatio.-1", .35) // Arcane Comet
        addFix(8229, "RechargeTime", 20) // Arcane Comet
        addFix(8229, "RechargeTimeMin", 8) // Arcane Comet
        addFix(8229, "PercentRefund*100", 20) // Arcane Comet
        addFix(8229, "AoEPercentRefund*100", 10) // Arcane Comet
        addFix(8229, "DotPercentRefund*100", 5) // Arcane Comet

        addFix(8321, "ExcessCostPenaltyFlat", 50) // Future's Market

        addFix(8242, "BonusTenacity*100", 15) // Unflinching
        addFix(8242, "BuffDuration", 10) // Unflinching
        addFix(8242, "PersistTenacity*100", 10) // Unflinching

        addFix(8243, "StartingCDR", 5) // The Ultimate Hat
        addFix(8243, "CDChunkPerStack", 2) // The Ultimate Hat
        addFix(8243, "MaxStacks", 5) // The Ultimate Hat

        addFix(8446, "TotalDemolishTime", 4) // Demolish
        addFix(8446, "DistanceToTower", 600) // Demolish
        addFix(8446, "OutputDamagePerStack", 125) // Demolish
        addFix(8446, "MaxHealthPercentDamage.0 * 100", 30) // Demolish
        addFix(8446, "CooldownSeconds", 45) // Demolish

        addFix(8128, "ONHDuration", 20) // Dark Harvest
        addFix(8128, "ONHDurationLong", 300) // Dark Harvest
        addFix(8128, "SoulsRequiredForIncreasedDuration", 150) // Dark Harvest
        addFix(8128, "DamageMin", 40) // Dark Harvest
        addFix(8128, "DamageMax", 80) // Dark Harvest
        addFix(8128, "ADRatio.2", 0.25) // Dark Harvest
        addFix(8128, "APRatio.1", 0.2) // Dark Harvest
        addFix(8128, "champstacks", 6) // Dark Harvest
        addFix(8128, "monsterstacks", 2) // Dark Harvest
        addFix(8128, "minionstacks", 4) // Dark Harvest

        addFix(8326, "ShardFirstMinutes", 2) // Unsealed Spellbook
        addFix(8326, "ShardRechargeMinutes", 6) // Unsealed Spellbook
        addFix(8326, "Maxshards", 2) // Unsealed Spellbook
        addFix(8326, "ShardCost", 1) // Unsealed Spellbook
        addFix(8326, "SummonerCDR.0*100", 15) // Unsealed Spellbook

        addFix(8444, "RegenPercentMax.0*100", 4) // Second Wind
        addFix(8444, "RegenFlat.0", 6) // Second Wind
        addFix(8444, "RegenSeconds", 10) // Second Wind

        addFix(8126, "DamageIncMin", 12) // Cheap Shot
        addFix(8126, "DamageIncMax", 30) // Cheap Shot
        addFix(8126, "Cooldown", 4) // Cheap Shot

        addFix(8005, "HitsRequired", 3) // Press the Attack
        addFix(8005, "MinDamage", 40) // Press the Attack
        addFix(8005, "MaxDamage", 180) // Press the Attack
        addFix(8005, "AmpPotencyStartSelf.0*100", 8) // Press the Attack
        addFix(8005, "AmpPotencyMaxSelf.0*100", 12) // Press the Attack
        addFix(8005, "AmpDuration", 6) // Press the Attack

        addFix(8120, "Cooldown", 3) // Ghost Poro

        addFix(8439, "FlatResists", 70) // Aftershock
        addFix(8439, "FlatResistMaximum", 120) // Aftershock
        addFix(8439, "DelayBeforeBurst.1", 2.5) // Aftershock
        addFix(8439, "StartingBaseDamage", 10) // Aftershock
        addFix(8439, "MaxBaseDamage", 120) // Aftershock
        addFix(8439, "HealthRatio.-1", 3) // Aftershock
        addFix(8439, "DamageRatioAD*100", 15) // Aftershock
        addFix(8439, "DamageRatioAP*100", 10) // Aftershock
        addFix(8439, "Cooldown", 35) // Aftershock

        addFix(8316, "GainedMinionKillers", 6) // Minion Dematerializer
        addFix(8316, "InitialCooldown", 155) // Minion Dematerializer
        addFix(8316, "DamageBonusForAnyAbsorbed.0*100", 4) // Minion Dematerializer
        addFix(8316, "DamageBonusPerAdditionalAbsorbed.0*100", 1) // Minion Dematerializer

        addFix(8437, "TriggerTime", 4) // Grasp of the Undying
        addFix(8437, "PercentHealthDamage.0", 4) // Grasp of the Undying
        addFix(8437, "PercentHealthHeal.0*100", 2) // Grasp of the Undying
        addFix(8437, "MaxHealthPerProc", 5) // Grasp of the Undying
        addFix(8437, "RangedPenaltyMod*100", 40) // Grasp of the Undying
    }

    private fun getFixFor(perkId: String, key: String): String? {
        val perkKeyDict = fixDict[perkId] ?: return null

        val o = perkKeyDict[key]
        return o as? String ?: if (o is Int || o is Double) {
            o.toString()
        } else if (o == null) {
            null
        } else {
            Log.d(TAG, "Unsupported type: " + o.javaClass.toString())
            null
        }
    }

    fun fix(toFix: JSONObject) {
        makeFixDict()

        val allRunes = toFix.getJSONObject("data")
        val keys = allRunes.keySet()

        val sb = StringBuffer()
        for (k in keys) {

            val rune = allRunes.getJSONObject(k)
            val desc = rune.getString("desc")
            val runeName = rune.getString("name")

            val matcher = SPECIAL_VARIABLE_PATTERN.matcher(desc)

            sb.setLength(0)
            while (matcher.find()) {
                val varName = matcher.group(1)

                val fix = getFixFor(k, varName)

                if (fix != null) {
                    matcher.appendReplacement(sb, fix)
                } else {
                    Log.d(TAG, String.format("addFix(%s, \"%s\", null); // %s", k, varName, runeName))
                }
            }
            matcher.appendTail(sb)

            rune.put("desc", sb.toString())
        }
    }
}
