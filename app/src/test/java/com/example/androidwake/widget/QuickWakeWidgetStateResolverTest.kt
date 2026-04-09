package com.example.androidwake.widget

import com.example.androidwake.FakeWakeRepository
import com.example.androidwake.FakeWifiIdentityProvider
import com.example.androidwake.domain.NetworkIdentity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickWakeWidgetStateResolverTest {
    @Test
    fun resolve_whenNetworkIsNotApproved_returnsNonApprovedState() = runTest {
        val repository = FakeWakeRepository()
        val wifi = FakeWifiIdentityProvider().apply {
            identity = NetworkIdentity("Cafe", "AA:BB:CC:DD:EE:FF")
        }

        val resolver = QuickWakeWidgetStateResolver(repository, wifi)
        val state = resolver.resolve()

        assertTrue(state is QuickWakeWidgetState.NonApproved)
    }

    @Test
    fun resolve_whenApprovedAndNoMachines_returnsApprovedStateWithEmptyMachines() = runTest {
        val repository = FakeWakeRepository()
        val network = repository.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val wifi = FakeWifiIdentityProvider().apply {
            identity = NetworkIdentity(network.ssid, network.bssid)
        }

        val resolver = QuickWakeWidgetStateResolver(repository, wifi)
        val state = resolver.resolve()

        assertTrue(state is QuickWakeWidgetState.Approved)
        val approved = state as QuickWakeWidgetState.Approved
        assertTrue(approved.machines.isEmpty())
    }

    @Test
    fun resolve_whenApprovedAndMachinesExist_returnsApprovedStateWithMachines() = runTest {
        val repository = FakeWakeRepository()
        val network = repository.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        repository.seedMachine(network.id, "Desktop", "11:22:33:44:55:66")
        repository.seedMachine(network.id, "NAS", "AA:BB:CC:DD:EE:10")
        val wifi = FakeWifiIdentityProvider().apply {
            identity = NetworkIdentity(network.ssid, network.bssid)
        }

        val resolver = QuickWakeWidgetStateResolver(repository, wifi)
        val state = resolver.resolve()

        assertTrue(state is QuickWakeWidgetState.Approved)
        val approved = state as QuickWakeWidgetState.Approved
        assertEquals(2, approved.machines.size)
        assertEquals("Desktop", approved.machines[0].name)
    }
}
