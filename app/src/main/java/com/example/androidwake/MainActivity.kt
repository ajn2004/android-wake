package com.example.androidwake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.androidwake.app.AppServices
import com.example.androidwake.ui.AppViewModel
import com.example.androidwake.ui.AppViewModelFactory
import com.example.androidwake.ui.WakeApp
import com.example.androidwake.widget.QuickWakeWidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        QuickWakeWidgetUpdater.requestUpdate(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AppServices.wakeRepository(applicationContext)
        val wifiProvider = AppServices.wifiIdentityProvider(applicationContext)
        val wolSender = AppServices.wolSender()

        setContent {
            val navController = rememberNavController()
            val vm: AppViewModel = viewModel(
                factory = AppViewModelFactory(
                    repository = repository,
                    wifiIdentityProvider = wifiProvider,
                    wolSender = wolSender,
                    onDataChanged = { QuickWakeWidgetUpdater.requestUpdate(applicationContext) },
                )
            )
            WakeApp(navController = navController, viewModel = vm)
        }
    }
}
