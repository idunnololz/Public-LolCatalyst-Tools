package com.ggstudios.tools.datafixer.riot_ddragon

@Suppress("unused")
class ReforgedRunePathDto {
    val id: Int = 0
    val key: String? = null
    val name: String? = null
    val icon: String? = null

    val slots: List<ReforgedRuneSlotDto>? = null

    class ReforgedRuneSlotDto {
        val runes: List<ReforgedRuneDto>? = null
    }

    class ReforgedRuneDto {
        val runePathName: String? = null
        val runePathId: Int = 0
        val name: String? = null
        val id: Int = 0
        val key: String? = null
        val shortDesc: String? = null
        val longDesc: String? = null
        val icon: String? = null
    }
}
