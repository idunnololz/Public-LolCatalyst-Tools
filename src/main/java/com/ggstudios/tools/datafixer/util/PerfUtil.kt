package com.ggstudios.tools.datafixer.util

object PerfUtil {
    private var lastTime: Long = 0

    fun startTimer() {
        lastTime = System.currentTimeMillis()
    }

    fun stopTimer(): Long = System.currentTimeMillis() - lastTime

    fun lap(): Long {
        val curTime = System.currentTimeMillis()
        val elapsed = curTime - lastTime
        lastTime = curTime
        return elapsed
    }

    fun stopTimerAndLog() {
        println("Operation took: ${stopTimer()}")
    }
}