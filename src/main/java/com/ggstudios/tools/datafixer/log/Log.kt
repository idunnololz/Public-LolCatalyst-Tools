package com.ggstudios.tools.datafixer.log

object Log {
    fun d(tag: String, message: String) {
        println(String.format("D/%s: %s", tag, message))
    }

    fun e(tag: String, message: String) {
        System.err.println(String.format("D/%s: %s", tag, message))
    }
}
