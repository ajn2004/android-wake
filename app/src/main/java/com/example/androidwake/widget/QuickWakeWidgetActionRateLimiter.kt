package com.example.androidwake.widget

object QuickWakeWidgetActionRateLimiter {
    private const val MIN_INTERVAL_MS = 1_000L
    private var lastWakeAllMs: Long = 0L
    private val lastWakeMachineMsByMac = mutableMapOf<String, Long>()

    @Synchronized
    fun shouldAllowWakeAll(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (nowMs - lastWakeAllMs < MIN_INTERVAL_MS) return false
        lastWakeAllMs = nowMs
        return true
    }

    @Synchronized
    fun shouldAllowWakeMachine(mac: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val last = lastWakeMachineMsByMac[mac] ?: 0L
        if (nowMs - last < MIN_INTERVAL_MS) return false
        lastWakeMachineMsByMac[mac] = nowMs
        return true
    }
}
