package com.example.androidwake

import com.example.androidwake.data.AddMachineResult
import com.example.androidwake.data.WakeRepository
import com.example.androidwake.domain.ApprovedNetwork
import com.example.androidwake.domain.MacAddress
import com.example.androidwake.domain.Machine
import com.example.androidwake.domain.NetworkIdentity
import com.example.androidwake.domain.WifiIdentityProvider
import com.example.androidwake.domain.WolSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class FakeWakeRepository : WakeRepository {
    private var nextNetworkId = 1L
    private var nextMachineId = 1L

    private val approvedNetworks = MutableStateFlow<List<ApprovedNetwork>>(emptyList())
    private val machines = MutableStateFlow<List<Machine>>(emptyList())

    override fun observeApprovedNetworks(): Flow<List<ApprovedNetwork>> = approvedNetworks

    override fun observeMachinesForNetwork(networkId: Long): Flow<List<Machine>> {
        return machines.map { all -> all.filter { it.networkId == networkId } }
    }

    override suspend fun addApprovedNetwork(ssidRaw: String, bssidRaw: String): Result<Unit> {
        val ssid = ssidRaw.trim()
        val bssid = MacAddress.normalize(bssidRaw)
            ?: return Result.failure(IllegalArgumentException("BSSID must be a valid MAC address."))
        if (ssid.isBlank()) {
            return Result.failure(IllegalArgumentException("SSID must be 1-32 characters."))
        }

        val exists = approvedNetworks.value.any { it.ssid == ssid && it.bssid == bssid }
        if (!exists) {
            val network = ApprovedNetwork(nextNetworkId++, ssid, bssid)
            approvedNetworks.value = approvedNetworks.value + network
        }
        return Result.success(Unit)
    }

    override suspend fun removeApprovedNetwork(networkId: Long) {
        approvedNetworks.value = approvedNetworks.value.filterNot { it.id == networkId }
        machines.value = machines.value.filterNot { it.networkId == networkId }
    }

    override suspend fun findApprovedNetwork(identity: NetworkIdentity): ApprovedNetwork? {
        return approvedNetworks.value.find { it.ssid == identity.ssid && it.bssid == identity.bssid }
    }

    override suspend fun addMachineToNetwork(
        networkId: Long,
        macRaw: String,
        nameRaw: String,
    ): Result<AddMachineResult> {
        val normalizedMac = MacAddress.normalize(macRaw)
            ?: return Result.failure(IllegalArgumentException("Invalid MAC address."))
        val resolvedName = nameRaw.trim().ifBlank { MacAddress.defaultMachineName(normalizedMac) }.take(50)

        val existing = machines.value.find { it.macAddress == normalizedMac }
        if (existing != null) {
            if (existing.networkId == networkId) {
                return Result.success(AddMachineResult.AlreadyExistsOnSameNetwork(existing))
            }
            return Result.success(
                AddMachineResult.DuplicateOnDifferentNetwork(
                    machineId = existing.id,
                    existingMachineName = existing.name,
                    normalizedMac = normalizedMac,
                    currentNetworkId = networkId,
                    resolvedName = resolvedName,
                )
            )
        }

        val machine = Machine(
            id = nextMachineId++,
            networkId = networkId,
            name = resolvedName,
            macAddress = normalizedMac,
        )
        machines.value = machines.value + machine
        return Result.success(AddMachineResult.Added(machine))
    }

    override suspend fun moveMachineToNetwork(
        machineId: Long,
        targetNetworkId: Long,
        machineName: String,
    ): Result<Unit> {
        machines.value = machines.value.map {
            if (it.id == machineId) it.copy(networkId = targetNetworkId, name = machineName) else it
        }
        return Result.success(Unit)
    }

    override suspend fun updateMachine(
        machineId: Long,
        networkId: Long,
        macRaw: String,
        nameRaw: String,
    ): Result<Unit> {
        val normalizedMac = MacAddress.normalize(macRaw)
            ?: return Result.failure(IllegalArgumentException("Invalid MAC address."))
        val resolvedName = nameRaw.trim().ifBlank { MacAddress.defaultMachineName(normalizedMac) }.take(50)

        val duplicate = machines.value.find { it.id != machineId && it.macAddress == normalizedMac }
        if (duplicate != null) {
            return if (duplicate.networkId == networkId) {
                Result.failure(IllegalArgumentException("A machine with this MAC already exists on this network."))
            } else {
                Result.failure(IllegalArgumentException("A machine with this MAC already exists on another network."))
            }
        }

        machines.value = machines.value.map {
            if (it.id == machineId) it.copy(name = resolvedName, macAddress = normalizedMac) else it
        }
        return Result.success(Unit)
    }

    override suspend fun removeMachine(machineId: Long) {
        machines.value = machines.value.filterNot { it.id == machineId }
    }

    suspend fun seedNetwork(ssid: String, bssid: String): ApprovedNetwork {
        addApprovedNetwork(ssid, bssid)
        return approvedNetworks.value.last()
    }

    suspend fun seedMachine(networkId: Long, name: String, macAddress: String): Machine {
        val normalizedMac = MacAddress.normalize(macAddress)
            ?: error("Invalid seed MAC")
        val machine = Machine(nextMachineId++, networkId, name, normalizedMac)
        machines.value = machines.value + machine
        return machine
    }
}

class FakeWifiIdentityProvider : WifiIdentityProvider {
    var identity: NetworkIdentity? = null

    override fun getCurrentIdentity(): NetworkIdentity? = identity
}

class RecordingWolSender : WolSender {
    val sentMacs = mutableListOf<String>()

    override suspend fun sendMagicPacket(normalizedMac: String) {
        sentMacs += normalizedMac
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
