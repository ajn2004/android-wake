package com.example.androidwake.widget

import com.example.androidwake.FakeWakeRepository
import com.example.androidwake.FakeWifiIdentityProvider
import com.example.androidwake.RecordingWolSender
import com.example.androidwake.domain.NetworkIdentity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickWakeWidgetActionHandlerTest {
    @Test
    fun wakeAll_whenApproved_sendsForCurrentApprovedNetworkMachinesOnly() = runTest {
        val repository = FakeWakeRepository()
        val home = repository.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        val work = repository.seedNetwork("WorkWiFi", "AA:BB:CC:DD:EE:02")
        repository.seedMachine(home.id, "Home Desktop", "11:22:33:44:55:66")
        repository.seedMachine(home.id, "Home NAS", "AA:BB:CC:DD:EE:10")
        repository.seedMachine(work.id, "Work PC", "AA:BB:CC:DD:EE:11")

        val wifi = FakeWifiIdentityProvider().apply {
            identity = NetworkIdentity(home.ssid, home.bssid)
        }
        val wolSender = RecordingWolSender()
        val resolver = QuickWakeWidgetStateResolver(repository, wifi)
        val handler = QuickWakeWidgetActionHandler(resolver, wolSender)

        handler.wakeAll()

        assertEquals(listOf("11:22:33:44:55:66", "AA:BB:CC:DD:EE:10"), wolSender.sentMacs)
    }

    @Test
    fun wakeMachineByMac_whenApproved_sendsOnlyRequestedMachine() = runTest {
        val repository = FakeWakeRepository()
        val home = repository.seedNetwork("HomeWiFi", "AA:BB:CC:DD:EE:01")
        repository.seedMachine(home.id, "Home Desktop", "11:22:33:44:55:66")
        repository.seedMachine(home.id, "Home NAS", "AA:BB:CC:DD:EE:10")

        val wifi = FakeWifiIdentityProvider().apply {
            identity = NetworkIdentity(home.ssid, home.bssid)
        }
        val wolSender = RecordingWolSender()
        val resolver = QuickWakeWidgetStateResolver(repository, wifi)
        val handler = QuickWakeWidgetActionHandler(resolver, wolSender)

        handler.wakeMachineByMac("AA:BB:CC:DD:EE:10")

        assertEquals(listOf("AA:BB:CC:DD:EE:10"), wolSender.sentMacs)
    }
}
