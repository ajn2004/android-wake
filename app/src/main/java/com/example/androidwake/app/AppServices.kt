package com.example.androidwake.app

import android.content.Context
import com.example.androidwake.data.AppDatabase
import com.example.androidwake.data.RoomWakeRepository
import com.example.androidwake.data.WakeRepository
import com.example.androidwake.domain.WifiIdentityProvider
import com.example.androidwake.domain.WolSender
import com.example.androidwake.network.AndroidWifiIdentityProvider
import com.example.androidwake.network.UdpWolSender

object AppServices {
    fun wakeRepository(context: Context): WakeRepository {
        val db = AppDatabase.getInstance(context.applicationContext)
        return RoomWakeRepository(db.approvedNetworkDao(), db.machineDao())
    }

    fun wifiIdentityProvider(context: Context): WifiIdentityProvider {
        return AndroidWifiIdentityProvider(context.applicationContext)
    }

    fun wolSender(): WolSender = UdpWolSender()
}
