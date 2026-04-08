package com.example.androidwake.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApprovedNetworkDao {
    @Query("SELECT * FROM approved_networks ORDER BY ssid ASC")
    fun observeAll(): Flow<List<ApprovedNetworkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ApprovedNetworkEntity): Long

    @Query("DELETE FROM approved_networks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM approved_networks WHERE ssid = :ssid AND bssid = :bssid LIMIT 1")
    suspend fun getBySsidAndBssid(ssid: String, bssid: String): ApprovedNetworkEntity?
}

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines WHERE networkId = :networkId ORDER BY name ASC")
    fun observeByNetworkId(networkId: Long): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getByMacAddress(macAddress: String): MachineEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MachineEntity): Long

    @Query("UPDATE machines SET networkId = :networkId, name = :name WHERE id = :id")
    suspend fun updateNetworkAndName(id: Long, networkId: Long, name: String)

    @Query("UPDATE machines SET name = :name, macAddress = :macAddress WHERE id = :id")
    suspend fun updateNameAndMacById(id: Long, name: String, macAddress: String)

    @Query("DELETE FROM machines WHERE id = :id")
    suspend fun deleteById(id: Long)
}
