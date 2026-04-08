package com.example.androidwake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch

private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val ADD_MACHINE_ROUTE = "add-machine"

@Composable
fun WakeApp(
    navController: NavHostController,
    viewModel: AppViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage ?: return@LaunchedEffect
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            composable(HOME_ROUTE) {
                HomeScreen(
                    state = state,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenAddMachine = { navController.navigate(ADD_MACHINE_ROUTE) },
                    onWakeMachine = viewModel::wakeMachine,
                    onWakeAll = viewModel::wakeAllCurrentNetworkMachines,
                    onRefresh = viewModel::refreshNetworkIdentity,
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onAddNetwork = viewModel::addApprovedNetwork,
                    onRemoveNetwork = viewModel::removeApprovedNetwork,
                )
            }
            composable(ADD_MACHINE_ROUTE) {
                AddMachineScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSave = { mac, name ->
                        viewModel.addMachine(mac = mac, name = name)
                    },
                    onConfirmMove = viewModel::confirmMoveMachine,
                    onCancelMove = viewModel::cancelPendingMove,
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    state: AppUiState,
    onOpenSettings: () -> Unit,
    onOpenAddMachine: () -> Unit,
    onWakeMachine: (com.example.androidwake.domain.Machine) -> Unit,
    onWakeAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Android Wake", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Wake packets are sent only on your current local Wi-Fi. Remote/internet wake is not supported.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onRefresh) { Text("Refresh Network") }

        when (val mode = state.homeMode) {
            is HomeMode.NonApproved -> {
                Text(mode.reason)
                state.identity?.let { identity ->
                    Text("Connected: ${identity.ssid} (${identity.bssid})")
                }
                Button(onClick = onOpenSettings) { Text("Manage Approved Networks") }
            }

            is HomeMode.Approved -> {
                Text(
                    text = "Connected to approved WLAN: ${mode.network.ssid}",
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onWakeAll) { Text("Wake All Machines") }
                    Button(onClick = onOpenAddMachine) { Text("Add Machine") }
                }
                Button(onClick = onOpenSettings) { Text("Network Settings") }

                Divider()
                Text("Wake specific machine")
                if (mode.machines.isEmpty()) {
                    Text("No machines registered for this network.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mode.machines, key = { it.id }) { machine ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(machine.name, fontWeight = FontWeight.Medium)
                                    Text(machine.macAddress, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { onWakeMachine(machine) }) {
                                    Text("Wake")
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onAddNetwork: (ssid: String, bssid: String) -> Unit,
    onRemoveNetwork: (Long) -> Unit,
) {
    var ssid by remember { mutableStateOf("") }
    var bssid by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Approved Networks", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Only devices connected to an approved SSID + BSSID can use wake actions.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SSID") },
            singleLine = true,
        )

        OutlinedTextField(
            value = bssid,
            onValueChange = { bssid = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("BSSID (AA:BB:CC:DD:EE:FF)") },
            singleLine = true,
        )

        Button(onClick = {
            onAddNetwork(ssid, bssid)
            ssid = ""
            bssid = ""
        }) {
            Text("Add Approved Network")
        }

        Divider()
        Text("Configured Networks")
        if (state.approvedNetworks.isEmpty()) {
            Text("No approved networks saved yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.approvedNetworks, key = { it.id }) { network ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(network.ssid, fontWeight = FontWeight.Medium)
                            Text(network.bssid, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onRemoveNetwork(network.id) }) {
                            Text("Remove")
                        }
                    }
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun AddMachineScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSave: (mac: String, name: String) -> Unit,
    onConfirmMove: () -> Unit,
    onCancelMove: () -> Unit,
) {
    var mac by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add Machine", style = MaterialTheme.typography.headlineSmall)
        Text("Machine is auto-associated with the currently connected approved SSID.")
        Text(
            "Wake-on-LAN works only if this phone and target machine are on the same local network.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = mac,
            onValueChange = { mac = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("MAC Address") },
            singleLine = true,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name (optional)") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(mac, name) }) { Text("Save Machine") }
            Button(onClick = onBack) { Text("Back") }
        }
    }

    if (state.pendingMove != null) {
        AlertDialog(
            onDismissRequest = onCancelMove,
            title = { Text("Duplicate MAC detected") },
            text = {
                Text(
                    "A machine (${state.pendingMove.existingName}) with this MAC already exists on another network. Move it to current network?"
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmMove) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelMove) {
                    Text("Cancel")
                }
            },
        )
    }
}
