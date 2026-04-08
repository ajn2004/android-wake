package com.example.androidwake.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacAddressTest {
    @Test
    fun normalize_accepts_common_mac_formats_and_rejects_invalid_values() {
        assertEquals("AA:BB:CC:DD:EE:FF", MacAddress.normalize("aa-bb-cc-dd-ee-ff"))
        assertEquals("AA:BB:CC:DD:EE:FF", MacAddress.normalize("aabbccddeeff"))
        assertEquals("AA:BB:CC:DD:EE:FF", MacAddress.normalize("AA:BB:CC:DD:EE:FF"))

        assertNull(MacAddress.normalize(""))
        assertNull(MacAddress.normalize("AA:BB:CC:DD:EE"))
        assertNull(MacAddress.normalize("GG:BB:CC:DD:EE:FF"))
        assertNull(MacAddress.normalize("FF:FF:FF:FF:FF:FF"))
        assertNull(MacAddress.normalize("00:00:00:00:00:00"))
    }
}
