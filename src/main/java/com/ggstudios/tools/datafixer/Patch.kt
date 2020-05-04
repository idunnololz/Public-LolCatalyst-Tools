package com.ggstudios.tools.datafixer

import org.apache.commons.io.FileUtils

import java.io.File
import java.io.IOException

object Patch {

    /**
     * Some of the thumbnails provided by Riot are wrong... we will copy the correct ones to the
     * 'res' folder.
     */
    private fun patchItemThumbs() {
        val source = File("patch_data/")
        val dest = File(Util.BASE_DIR + "item_thumb/")
        try {
            FileUtils.copyDirectory(source, dest)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun doPatch() {
        patchItemThumbs()
        pln("Patch complete")
    }
}
