package com.example.androidwake.widget

import com.example.androidwake.domain.WolSender

sealed interface QuickWakeActionResult {
    data class Success(val count: Int) : QuickWakeActionResult
    data object NotApproved : QuickWakeActionResult
    data object MachineNotFound : QuickWakeActionResult
}

class QuickWakeWidgetActionHandler(
    private val stateResolver: QuickWakeWidgetStateResolver,
    private val wolSender: WolSender,
) {
    suspend fun wakeAll(): QuickWakeActionResult {
        val state = stateResolver.resolve()
        if (state !is QuickWakeWidgetState.Approved) {
            return QuickWakeActionResult.NotApproved
        }

        state.machines.forEach { machine ->
            wolSender.sendMagicPacket(machine.macAddress)
        }
        return QuickWakeActionResult.Success(state.machines.size)
    }

    suspend fun wakeMachineByMac(normalizedMac: String): QuickWakeActionResult {
        val state = stateResolver.resolve()
        if (state !is QuickWakeWidgetState.Approved) {
            return QuickWakeActionResult.NotApproved
        }

        val machine = state.machines.find { it.macAddress == normalizedMac }
            ?: return QuickWakeActionResult.MachineNotFound

        wolSender.sendMagicPacket(machine.macAddress)
        return QuickWakeActionResult.Success(count = 1)
    }
}
