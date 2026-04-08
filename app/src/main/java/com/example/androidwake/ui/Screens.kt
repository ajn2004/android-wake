package com.example.androidwake.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.androidwake.domain.MacAddress
import com.example.androidwake.domain.Machine
import com.example.androidwake.domain.WifiPermissionPolicy
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi

private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val ADD_MACHINE_ROUTE = "add-machine"
private const val EDIT_MACHINE_ROUTE = "edit-machine/{machineId}"

@Composable
fun WakeApp(
    navController: NavHostController,
    viewModel: AppViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var permissionPromptLaunched by rememberSaveable { mutableStateOf(false) }
    val runtimeWifiPermissions = remember {
        WifiPermissionPolicy.requiredRuntimePermissionsForSdk(Build.VERSION.SDK_INT)
    }
    val hasRequiredWifiPermissions = runtimeWifiPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshNetworkIdentity()
    }

    fun requestWifiPermissions() {
        if (runtimeWifiPermissions.isNotEmpty()) {
            permissionLauncher.launch(runtimeWifiPermissions.toTypedArray())
        }
    }

    val shouldShowPermissionPrompt = !hasRequiredWifiPermissions && state.identity == null

    LaunchedEffect(shouldShowPermissionPrompt) {
        if (shouldShowPermissionPrompt && !permissionPromptLaunched) {
            permissionPromptLaunched = true
            requestWifiPermissions()
        }
    }

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage ?: return@LaunchedEffect
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(state.returnToHomeSignal) {
        if (state.returnToHomeSignal <= 0) return@LaunchedEffect
        val route = navController.currentBackStackEntry?.destination?.route
        if (route == ADD_MACHINE_ROUTE || route == EDIT_MACHINE_ROUTE) {
            navController.popBackStack(HOME_ROUTE, inclusive = false)
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
                    wifiPermissionsMissing = shouldShowPermissionPrompt,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenAddMachine = { navController.navigate(ADD_MACHINE_ROUTE) },
                    onWakeMachine = viewModel::wakeMachine,
                    onWakeAll = viewModel::wakeAllCurrentNetworkMachines,
                    onEditMachine = { machine -> navController.navigate("edit-machine/${machine.id}") },
                    onRemoveMachine = viewModel::removeMachine,
                    onRequestWifiPermissions = { requestWifiPermissions() },
                    onRefresh = viewModel::refreshNetworkIdentity,
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onAddNetwork = viewModel::addApprovedNetwork,
                    onAddCurrentNetwork = viewModel::addCurrentNetwork,
                    onRemoveNetwork = viewModel::removeApprovedNetwork,
                )
            }
            composable(ADD_MACHINE_ROUTE) {
                AddMachineScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    initialMac = "",
                    initialName = "",
                    onSave = { mac, name ->
                        viewModel.addMachine(mac = mac, name = name)
                    },
                    onConfirmMove = viewModel::confirmMoveMachine,
                    onCancelMove = viewModel::cancelPendingMove,
                )
            }
            composable(
                route = EDIT_MACHINE_ROUTE,
                arguments = listOf(navArgument("machineId") { type = NavType.LongType }),
            ) { entry ->
                val machineId = entry.arguments?.getLong("machineId")
                val machine = ((state.homeMode as? HomeMode.Approved)?.machines ?: emptyList())
                    .find { it.id == machineId }
                EditMachineScreen(
                    machine = machine,
                    onBack = { navController.popBackStack() },
                    onSave = { mac, name ->
                        if (machine != null) {
                            viewModel.updateMachine(machine.id, mac, name)
                        }
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HomeScreen(
    state: AppUiState,
    wifiPermissionsMissing: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAddMachine: () -> Unit,
    onWakeMachine: (com.example.androidwake.domain.Machine) -> Unit,
    onWakeAll: () -> Unit,
    onEditMachine: (Machine) -> Unit,
    onRemoveMachine: (Long) -> Unit,
    onRequestWifiPermissions: () -> Unit,
    onRefresh: () -> Unit,
) {
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var machinePendingRemoval by remember { mutableStateOf<Machine?>(null) }

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
                if (wifiPermissionsMissing) {
                    Button(onClick = onRequestWifiPermissions) { Text("Grant Wi-Fi Permissions") }
                }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { selectedMachine = machine },
                                    ),
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

    if (selectedMachine != null) {
        AlertDialog(
            onDismissRequest = { selectedMachine = null },
            title = { Text(selectedMachine?.name ?: "") },
            text = { Text("Choose an action for this computer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val machine = selectedMachine
                        selectedMachine = null
                        if (machine != null) onEditMachine(machine)
                    }
                ) {
                    Text("Edit computer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        machinePendingRemoval = selectedMachine
                        selectedMachine = null
                    }
                ) {
                    Text("Remove computer")
                }
            },
        )
    }

    if (machinePendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { machinePendingRemoval = null },
            title = { Text("Remove computer") },
            text = { Text("Remove ${machinePendingRemoval?.name} from this network?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = machinePendingRemoval?.id
                        machinePendingRemoval = null
                        if (id != null) onRemoveMachine(id)
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { machinePendingRemoval = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onAddNetwork: (ssid: String, bssid: String) -> Unit,
    onAddCurrentNetwork: () -> Unit,
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

        state.identity?.let { identity ->
            Text(
                "Current Wi-Fi: ${identity.ssid} (${identity.bssid})",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onAddCurrentNetwork) {
                Text("Add Current Network")
            }
        } ?: Text(
            "Current Wi-Fi network not detected.",
            style = MaterialTheme.typography.bodySmall,
        )

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
    initialMac: String,
    initialName: String,
    onSave: (mac: String, name: String) -> Unit,
    onConfirmMove: () -> Unit,
    onCancelMove: () -> Unit,
) {
    var mac by remember(initialMac) {
        mutableStateOf(
            TextFieldValue(
                text = initialMac,
                selection = TextRange(initialMac.length),
            )
        )
    }
    var name by remember(initialName) { mutableStateOf(initialName) }

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
            onValueChange = { value ->
                val (formatted, selection) = MacAddress.formatForInputWithSelection(
                    raw = value.text,
                    selectionStart = value.selection.start,
                )
                mac = TextFieldValue(
                    text = formatted,
                    selection = TextRange(selection),
                )
            },
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
            Button(onClick = { onSave(mac.text, name) }) { Text("Save Machine") }
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

@Composable
fun EditMachineScreen(
    machine: Machine?,
    onBack: () -> Unit,
    onSave: (mac: String, name: String) -> Unit,
) {
    if (machine == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Machine not found.")
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    var mac by remember(machine.id) {
        mutableStateOf(
            TextFieldValue(
                text = machine.macAddress,
                selection = TextRange(machine.macAddress.length),
            )
        )
    }
    var name by remember(machine.id) { mutableStateOf(machine.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Edit Machine", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = mac,
            onValueChange = { value ->
                val (formatted, selection) = MacAddress.formatForInputWithSelection(
                    raw = value.text,
                    selectionStart = value.selection.start,
                )
                mac = TextFieldValue(
                    text = formatted,
                    selection = TextRange(selection),
                )
            },
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
            Button(onClick = { onSave(mac.text, name) }) { Text("Save Changes") }
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
