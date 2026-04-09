package com.example.androidwake.widget

object QuickWakeWidgetRenderStateStore {
    private val policy = QuickWakeWidgetRenderStatePolicy()

    @Synchronized
    fun choose(current: QuickWakeWidgetState, nowMs: Long = System.currentTimeMillis()): QuickWakeWidgetState {
        return policy.choose(current, nowMs)
    }
}
