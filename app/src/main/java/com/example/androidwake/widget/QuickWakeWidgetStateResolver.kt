package com.example.androidwake.widget

import com.example.androidwake.data.WakeRepository
import com.example.androidwake.domain.WifiIdentityProvider
import kotlinx.coroutines.flow.first

class QuickWakeWidgetStateResolver(
    private val repository: WakeRepository,
    private val wifiIdentityProvider: WifiIdentityProvider,
) {
    suspend fun resolve(): QuickWakeWidgetState {
        val identity = wifiIdentityProvider.getCurrentIdentity()
            ?: return QuickWakeWidgetState.NonApproved(NOT_APPROVED_MESSAGE)

        val network = repository.findApprovedNetwork(identity)
            ?: return QuickWakeWidgetState.NonApproved(NOT_APPROVED_MESSAGE)

        val machines = repository.observeMachinesForNetwork(network.id).first()
        return QuickWakeWidgetState.Approved(network = network, machines = machines)
    }

    companion object {
        const val NOT_APPROVED_MESSAGE = "Not connected to an approved network."
    }
}
