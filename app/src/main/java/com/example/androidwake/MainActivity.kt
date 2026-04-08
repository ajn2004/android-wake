package com.example.androidwake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.androidwake.data.AppDatabase
import com.example.androidwake.data.RoomWakeRepository
import com.example.androidwake.network.AndroidWifiIdentityProvider
import com.example.androidwake.network.UdpWolSender
import com.example.androidwake.ui.AppViewModel
import com.example.androidwake.ui.AppViewModelFactory
import com.example.androidwake.ui.WakeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = RoomWakeRepository(db.approvedNetworkDao(), db.machineDao())
        val wifiProvider = AndroidWifiIdentityProvider(applicationContext)
        val wolSender = UdpWolSender()

        setContent {
            val navController = rememberNavController()
            val vm: AppViewModel = viewModel(
                factory = AppViewModelFactory(repository, wifiProvider, wolSender)
            )
            WakeApp(navController = navController, viewModel = vm)
        }
    }
}
