package com.example.androidwake.domain

data class ApprovedNetwork(
    val id: Long,
    val ssid: String,
    val bssid: String,
)

data class Machine(
    val id: Long,
    val networkId: Long,
    val name: String,
    val macAddress: String,
)

data class NetworkIdentity(
    val ssid: String,
    val bssid: String,
)
