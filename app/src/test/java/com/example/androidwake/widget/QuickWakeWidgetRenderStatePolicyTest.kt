package com.example.androidwake.widget

import com.example.androidwake.domain.ApprovedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickWakeWidgetRenderStatePolicyTest {
    @Test
    fun choose_whenTransientNonApprovedAfterApproved_returnsCachedApprovedWithinGraceWindow() {
        val approved = QuickWakeWidgetState.Approved(
            network = ApprovedNetwork(1L, "HomeWiFi", "AA:BB:CC:DD:EE:01"),
            machines = emptyList(),
        )

        val policy = QuickWakeWidgetRenderStatePolicy(gracePeriodMs = 15_000L)
        policy.choose(approved, nowMs = 1_000L)

        val rendered = policy.choose(
            QuickWakeWidgetState.NonApproved("Not connected to an approved network."),
            nowMs = 5_000L,
        )

        assertTrue(rendered is QuickWakeWidgetState.Approved)
    }

    @Test
    fun choose_whenNonApprovedBeyondGraceWindow_returnsNonApproved() {
        val approved = QuickWakeWidgetState.Approved(
            network = ApprovedNetwork(1L, "HomeWiFi", "AA:BB:CC:DD:EE:01"),
            machines = emptyList(),
        )

        val policy = QuickWakeWidgetRenderStatePolicy(gracePeriodMs = 1_000L)
        policy.choose(approved, nowMs = 1_000L)

        val rendered = policy.choose(
            QuickWakeWidgetState.NonApproved("Not connected to an approved network."),
            nowMs = 4_000L,
        )

        assertTrue(rendered is QuickWakeWidgetState.NonApproved)
        assertEquals("Not connected to an approved network.", (rendered as QuickWakeWidgetState.NonApproved).message)
    }
}
