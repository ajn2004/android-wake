package com.example.androidwake.ui

import com.example.androidwake.FakeWakeRepository
import com.example.androidwake.FakeWifiIdentityProvider
import com.example.androidwake.MainDispatcherRule
import com.example.androidwake.RecordingWolSender
import com.example.androidwake.domain.NetworkIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun duplicate_mac_on_different_network_sets_pending_move_prompt() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val work = repo.seedNetwork("WorkWiFi", "AA:BB:CC:DD:EE:02")
        repo.seedMachine(work.id, "Office PC", "11:22:33:44:55:66")

        wifi.identity = NetworkIdentity(home.ssid, home.bssid)
        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        vm.addMachine("11:22:33:44:55:66", "Home PC")
        advanceUntilIdle()

        val pending = vm.uiState.value.pendingMove
        assertNotNull(pending)
        assertEquals("11:22:33:44:55:66", pending?.normalizedMac)
        assertEquals(home.id, pending?.targetNetworkId)
        assertEquals("Home PC", pending?.resolvedName)
        assertNull(vm.uiState.value.statusMessage)
        collector.cancel()
    }

    @Test
    fun machine_list_filters_to_current_approved_ssid() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val work = repo.seedNetwork("WorkWiFi", "AA:BB:CC:DD:EE:02")
        repo.seedMachine(home.id, "Home Server", "AA:AA:AA:AA:AA:01")
        repo.seedMachine(work.id, "Work NAS", "AA:AA:AA:AA:AA:02")

        wifi.identity = NetworkIdentity(home.ssid, home.bssid)
        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        val homeMode = vm.uiState.value.homeMode as HomeMode.Approved
        assertEquals(1, homeMode.machines.size)
        assertEquals("Home Server", homeMode.machines.first().name)

        wifi.identity = NetworkIdentity(work.ssid, work.bssid)
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        val workMode = vm.uiState.value.homeMode as HomeMode.Approved
        assertEquals(1, workMode.machines.size)
        assertEquals("Work NAS", workMode.machines.first().name)
        collector.cancel()
    }

    @Test
    fun gating_switches_between_non_approved_and_approved_modes() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }

        wifi.identity = NetworkIdentity("Cafe", "AA:BB:CC:DD:EE:AA")
        vm.refreshNetworkIdentity()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.homeMode is HomeMode.NonApproved)

        wifi.identity = NetworkIdentity(home.ssid, home.bssid)
        vm.refreshNetworkIdentity()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.homeMode is HomeMode.Approved)
        collector.cancel()
    }

    @Test
    fun add_and_remove_approved_network_updates_settings_state() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()
        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }

        vm.addApprovedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.approvedNetworks.size)
        val networkId = vm.uiState.value.approvedNetworks.first().id

        vm.removeApprovedNetwork(networkId)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.approvedNetworks.size)
        collector.cancel()
    }

    @Test
    fun add_current_network_adds_connected_identity_to_approved_list() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()
        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }

        wifi.identity = NetworkIdentity("HomeWiFi", "AA:BB:CC:DD:EE:01")
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        vm.addCurrentNetwork()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.approvedNetworks.size)
        assertEquals("HomeWiFi", vm.uiState.value.approvedNetworks.first().ssid)
        assertEquals("AA:BB:CC:DD:EE:01", vm.uiState.value.approvedNetworks.first().bssid)
        collector.cancel()
    }

    @Test
    fun add_machine_and_wake_actions_send_magic_packets() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        wifi.identity = NetworkIdentity(home.ssid, home.bssid)

        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        vm.addMachine("11:22:33:44:55:66", "")
        advanceUntilIdle()

        val approved = vm.uiState.value.homeMode as HomeMode.Approved
        assertEquals(1, approved.machines.size)
        assertEquals("Machine 445566", approved.machines.first().name)

        vm.wakeMachine(approved.machines.first())
        advanceUntilIdle()

        vm.wakeAllCurrentNetworkMachines()
        advanceUntilIdle()

        assertEquals(listOf("11:22:33:44:55:66", "11:22:33:44:55:66"), wol.sentMacs)
        collector.cancel()
    }

    @Test
    fun add_machine_success_emits_return_to_home_signal() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        wifi.identity = NetworkIdentity(home.ssid, home.bssid)

        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.returnToHomeSignal)

        vm.addMachine("11:22:33:44:55:66", "Desktop")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.returnToHomeSignal)
        collector.cancel()
    }

    @Test
    fun update_machine_changes_saved_values() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val machine = repo.seedMachine(home.id, "Desktop", "11:22:33:44:55:66")
        wifi.identity = NetworkIdentity(home.ssid, home.bssid)

        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        vm.updateMachine(machine.id, "112233aabbcc", "Main PC")
        advanceUntilIdle()

        val approved = vm.uiState.value.homeMode as HomeMode.Approved
        assertEquals("Main PC", approved.machines.first().name)
        assertEquals("11:22:33:AA:BB:CC", approved.machines.first().macAddress)
        collector.cancel()
    }

    @Test
    fun remove_machine_deletes_it_from_current_network_list() = runTest {
        val repo = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider()
        val wol = RecordingWolSender()

        val home = repo.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val machine = repo.seedMachine(home.id, "Desktop", "11:22:33:44:55:66")
        wifi.identity = NetworkIdentity(home.ssid, home.bssid)

        val vm = AppViewModel(repo, wifi, wol, autoRefreshNetwork = false)
        val collector = backgroundScope.launch { vm.uiState.collect { } }
        vm.refreshNetworkIdentity()
        advanceUntilIdle()

        vm.removeMachine(machine.id)
        advanceUntilIdle()

        val approved = vm.uiState.value.homeMode as HomeMode.Approved
        assertTrue(approved.machines.isEmpty())
        collector.cancel()
    }
}
