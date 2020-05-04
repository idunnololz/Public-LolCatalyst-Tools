package com.ggstudios.tools.datafixer

fun pln(p: String) {
    println(p)
}

fun p(p: String) {
    print(p)
    System.out.flush()
}

object Util {
    const val BASE_DIR = "res/"
    const val BASE_APP_DIR = "app/src/main/res/"
    const val BASE_RES_DIR = "datafetcher/res"

    fun parseFirstInteger(str: String): Int {
        var i = 0
        var n = 0
        val len = str.length
        while (i < len) {
            val c = str[i]
            if (c in '0'..'9') break
            i++
        }
        while (i < len) {
            val c = str[i]
            if (c in '0'..'9') {
                n *= 10
                n += c - '0'
            } else {
                break
            }
            i++
        }
        return n
    }
}
