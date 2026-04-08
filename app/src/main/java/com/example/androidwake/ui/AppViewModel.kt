package com.example.androidwake.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.androidwake.data.AddMachineResult
import com.example.androidwake.data.WakeRepository
import com.example.androidwake.domain.ApprovedNetwork
import com.example.androidwake.domain.Machine
import com.example.androidwake.domain.NetworkIdentity
import com.example.androidwake.domain.WifiIdentityProvider
import com.example.androidwake.domain.WolSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeMode {
    data class NonApproved(val reason: String) : HomeMode
    data class Approved(val network: ApprovedNetwork, val machines: List<Machine>) : HomeMode
}

data class PendingMove(
    val machineId: Long,
    val targetNetworkId: Long,
    val normalizedMac: String,
    val resolvedName: String,
    val existingName: String,
)

data class AppUiState(
    val identity: NetworkIdentity? = null,
    val approvedNetworks: List<ApprovedNetwork> = emptyList(),
    val homeMode: HomeMode = HomeMode.NonApproved("You have to connect to an approved WLAN network."),
    val statusMessage: String? = null,
    val pendingMove: PendingMove? = null,
)

private data class UiCoreState(
    val identity: NetworkIdentity?,
    val approvedNetworks: List<ApprovedNetwork>,
    val homeMode: HomeMode,
    val statusMessage: String?,
)

class AppViewModel(
    private val repository: WakeRepository,
    private val wifiIdentityProvider: WifiIdentityProvider,
    private val wolSender: WolSender,
    private val autoRefreshNetwork: Boolean = true,
    private val refreshIntervalMs: Long = 5_000,
) : ViewModel() {

    private val identityFlow = MutableStateFlow<NetworkIdentity?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val pendingMoveFlow = MutableStateFlow<PendingMove?>(null)

    private val approvedNetworksFlow = repository.observeApprovedNetworks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val currentApprovedFlow = combine(identityFlow, approvedNetworksFlow) { identity, networks ->
        if (identity == null) return@combine null
        networks.find { it.ssid == identity.ssid && it.bssid.equals(identity.bssid, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val machinesFlow = currentApprovedFlow.flatMapLatest { network ->
        if (network == null) flowOf(emptyList()) else repository.observeMachinesForNetwork(network.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val uiCoreState = combine(
        identityFlow,
        approvedNetworksFlow,
        currentApprovedFlow,
        machinesFlow,
        statusMessageFlow,
    ) { identity, networks, currentApproved, machines, status ->
        val homeMode = if (currentApproved == null) {
            val reason = if (identity == null) {
                "You have to connect to a WLAN network and grant Wi-Fi permissions."
            } else {
                "Current WLAN is not approved."
            }
            HomeMode.NonApproved(reason)
        } else {
            HomeMode.Approved(currentApproved, machines)
        }

        UiCoreState(
            identity = identity,
            approvedNetworks = networks,
            homeMode = homeMode,
            statusMessage = status,
        )
    }

    val uiState: StateFlow<AppUiState> = combine(
        uiCoreState,
        pendingMoveFlow,
    ) { core, pendingMove ->
        AppUiState(
            identity = core.identity,
            approvedNetworks = core.approvedNetworks,
            homeMode = core.homeMode,
            statusMessage = core.statusMessage,
            pendingMove = pendingMove,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        refreshNetworkIdentity()
        if (autoRefreshNetwork) {
            viewModelScope.launch {
                while (true) {
                    delay(refreshIntervalMs)
                    refreshNetworkIdentity()
                }
            }
        }
    }

    fun refreshNetworkIdentity() {
        identityFlow.value = wifiIdentityProvider.getCurrentIdentity()
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    fun addApprovedNetwork(ssid: String, bssid: String) {
        viewModelScope.launch {
            val result = repository.addApprovedNetwork(ssid, bssid)
            statusMessageFlow.value = result.exceptionOrNull()?.message ?: "Approved network saved."
        }
    }

    fun removeApprovedNetwork(networkId: Long) {
        viewModelScope.launch {
            repository.removeApprovedNetwork(networkId)
            statusMessageFlow.value = "Approved network removed."
        }
    }

    fun addMachine(mac: String, name: String) {
        val approved = (uiState.value.homeMode as? HomeMode.Approved)?.network
        if (approved == null) {
            statusMessageFlow.value = "Connect to an approved network before adding machines."
            return
        }

        viewModelScope.launch {
            val result = repository.addMachineToNetwork(approved.id, mac, name)
            if (result.isFailure) {
                statusMessageFlow.value = result.exceptionOrNull()?.message
                return@launch
            }

            when (val addResult = result.getOrThrow()) {
                is AddMachineResult.Added -> {
                    pendingMoveFlow.value = null
                    statusMessageFlow.value = "Added ${addResult.machine.name}."
                }
                is AddMachineResult.AlreadyExistsOnSameNetwork -> {
                    pendingMoveFlow.value = null
                    statusMessageFlow.value = "Machine already exists on this network."
                }
                is AddMachineResult.DuplicateOnDifferentNetwork -> {
                    pendingMoveFlow.value = PendingMove(
                        machineId = addResult.machineId,
                        targetNetworkId = addResult.currentNetworkId,
                        normalizedMac = addResult.normalizedMac,
                        resolvedName = addResult.resolvedName,
                        existingName = addResult.existingMachineName,
                    )
                    statusMessageFlow.value = null
                }
            }
        }
    }

    fun cancelPendingMove() {
        pendingMoveFlow.value = null
    }

    fun confirmMoveMachine() {
        val pendingMove = pendingMoveFlow.value ?: return
        viewModelScope.launch {
            val result = repository.moveMachineToNetwork(
                machineId = pendingMove.machineId,
                targetNetworkId = pendingMove.targetNetworkId,
                machineName = pendingMove.resolvedName,
            )
            pendingMoveFlow.value = null
            statusMessageFlow.value = if (result.isSuccess) {
                "Moved ${pendingMove.normalizedMac} to current network."
            } else {
                result.exceptionOrNull()?.message
            }
        }
    }

    fun wakeMachine(machine: Machine) {
        viewModelScope.launch {
            runCatching { wolSender.sendMagicPacket(machine.macAddress) }
                .onSuccess { statusMessageFlow.value = "Wake sent to ${machine.name}." }
                .onFailure { statusMessageFlow.value = "Wake failed: ${it.message}" }
        }
    }

    fun wakeAllCurrentNetworkMachines() {
        val approvedHome = uiState.value.homeMode as? HomeMode.Approved ?: return
        viewModelScope.launch {
            var sentCount = 0
            approvedHome.machines.forEach { machine ->
                runCatching { wolSender.sendMagicPacket(machine.macAddress) }
                    .onSuccess { sentCount++ }
            }
            statusMessageFlow.value = "Wake sent to $sentCount machine(s)."
        }
    }
}

class AppViewModelFactory(
    private val repository: WakeRepository,
    private val wifiIdentityProvider: WifiIdentityProvider,
    private val wolSender: WolSender,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository, wifiIdentityProvider, wolSender) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
