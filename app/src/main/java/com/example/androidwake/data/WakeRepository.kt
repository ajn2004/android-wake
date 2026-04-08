package com.example.androidwake.data

import com.example.androidwake.domain.ApprovedNetwork
import com.example.androidwake.domain.MacAddress
import com.example.androidwake.domain.Machine
import com.example.androidwake.domain.NetworkIdentity
import com.example.androidwake.domain.NetworkIdentityValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AddMachineResult {
    data class Added(val machine: Machine) : AddMachineResult
    data class AlreadyExistsOnSameNetwork(val machine: Machine) : AddMachineResult
    data class DuplicateOnDifferentNetwork(
        val machineId: Long,
        val existingMachineName: String,
        val normalizedMac: String,
        val currentNetworkId: Long,
        val resolvedName: String,
    ) : AddMachineResult
}

interface WakeRepository {
    fun observeApprovedNetworks(): Flow<List<ApprovedNetwork>>
    fun observeMachinesForNetwork(networkId: Long): Flow<List<Machine>>

    suspend fun addApprovedNetwork(ssidRaw: String, bssidRaw: String): Result<Unit>
    suspend fun removeApprovedNetwork(networkId: Long)
    suspend fun findApprovedNetwork(identity: NetworkIdentity): ApprovedNetwork?

    suspend fun addMachineToNetwork(networkId: Long, macRaw: String, nameRaw: String): Result<AddMachineResult>
    suspend fun moveMachineToNetwork(machineId: Long, targetNetworkId: Long, machineName: String): Result<Unit>
}

class RoomWakeRepository(
    private val approvedNetworkDao: ApprovedNetworkDao,
    private val machineDao: MachineDao,
) : WakeRepository {

    override fun observeApprovedNetworks(): Flow<List<ApprovedNetwork>> {
        return approvedNetworkDao.observeAll().map { entities ->
            entities.map { ApprovedNetwork(it.id, it.ssid, it.bssid) }
        }
    }

    override fun observeMachinesForNetwork(networkId: Long): Flow<List<Machine>> {
        return machineDao.observeByNetworkId(networkId).map { entities ->
            entities.map { Machine(it.id, it.networkId, it.name, it.macAddress) }
        }
    }

    override suspend fun addApprovedNetwork(ssidRaw: String, bssidRaw: String): Result<Unit> {
        val ssid = NetworkIdentityValidator.normalizeSsid(ssidRaw)
            ?: return Result.failure(IllegalArgumentException("SSID must be 1-32 characters."))
        val bssid = NetworkIdentityValidator.normalizeBssid(bssidRaw)
            ?: return Result.failure(IllegalArgumentException("BSSID must be a valid MAC address."))

        approvedNetworkDao.insert(ApprovedNetworkEntity(ssid = ssid, bssid = bssid))
        return Result.success(Unit)
    }

    override suspend fun removeApprovedNetwork(networkId: Long) {
        approvedNetworkDao.deleteById(networkId)
    }

    override suspend fun findApprovedNetwork(identity: NetworkIdentity): ApprovedNetwork? {
        val network = approvedNetworkDao.getBySsidAndBssid(identity.ssid, identity.bssid) ?: return null
        return ApprovedNetwork(id = network.id, ssid = network.ssid, bssid = network.bssid)
    }

    override suspend fun addMachineToNetwork(
        networkId: Long,
        macRaw: String,
        nameRaw: String,
    ): Result<AddMachineResult> {
        val normalizedMac = MacAddress.normalize(macRaw)
            ?: return Result.failure(IllegalArgumentException("Invalid MAC address."))
        val resolvedName = nameRaw.trim().ifBlank { MacAddress.defaultMachineName(normalizedMac) }.take(50)

        val existing = machineDao.getByMacAddress(normalizedMac)
        if (existing != null) {
            val existingMachine = Machine(existing.id, existing.networkId, existing.name, existing.macAddress)
            if (existing.networkId == networkId) {
                return Result.success(AddMachineResult.AlreadyExistsOnSameNetwork(existingMachine))
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

        val id = machineDao.insert(
            MachineEntity(networkId = networkId, name = resolvedName, macAddress = normalizedMac)
        )
        return Result.success(AddMachineResult.Added(Machine(id, networkId, resolvedName, normalizedMac)))
    }

    override suspend fun moveMachineToNetwork(
        machineId: Long,
        targetNetworkId: Long,
        machineName: String,
    ): Result<Unit> {
        machineDao.updateNetworkAndName(machineId, targetNetworkId, machineName)
        return Result.success(Unit)
    }
}
