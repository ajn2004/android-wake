package com.example.androidwake.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacAddressTest {
    @Test
    fun format_for_input_auto_inserts_colon_separators() {
        assertEquals("11:22:33:44:55", MacAddress.formatForInput("1122334455"))
        assertEquals("11:22:33:44:55:66", MacAddress.formatForInput("11-22-33-44-55-66"))
        assertEquals("11:22:33:44:55:66", MacAddress.formatForInput("1122334455667788"))
        assertEquals("AA:BB:C", MacAddress.formatForInput("aabbc"))
        assertEquals("", MacAddress.formatForInput("zzzz"))
    }

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
