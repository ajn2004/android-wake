package com.example.androidwake.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "approved_networks",
    indices = [Index(value = ["ssid", "bssid"], unique = true)]
)
data class ApprovedNetworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val bssid: String,
)

@Entity(
    tableName = "machines",
    foreignKeys = [
        ForeignKey(
            entity = ApprovedNetworkEntity::class,
            parentColumns = ["id"],
            childColumns = ["networkId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["networkId"]), Index(value = ["macAddress"], unique = true)]
)
data class MachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val networkId: Long,
    val name: String,
    val macAddress: String,
)
