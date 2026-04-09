package com.example.androidwake.widget

class QuickWakeWidgetRenderStatePolicy(
    private val gracePeriodMs: Long = 15_000L,
) {
    private var lastApprovedState: QuickWakeWidgetState.Approved? = null
    private var lastApprovedAtMs: Long = 0L

    @Synchronized
    fun choose(current: QuickWakeWidgetState, nowMs: Long = System.currentTimeMillis()): QuickWakeWidgetState {
        return when (current) {
            is QuickWakeWidgetState.Approved -> {
                lastApprovedState = current
                lastApprovedAtMs = nowMs
                current
            }

            is QuickWakeWidgetState.NonApproved -> {
                val cached = lastApprovedState
                if (cached != null && nowMs - lastApprovedAtMs <= gracePeriodMs) {
                    cached
                } else {
                    current
                }
            }
        }
    }
}
