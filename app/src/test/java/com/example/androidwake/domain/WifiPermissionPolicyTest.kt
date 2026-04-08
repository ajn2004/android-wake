package com.example.androidwake.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiPermissionPolicyTest {
    @Test
    fun required_permissions_match_sdk_levels() {
        assertEquals(
            listOf("android.permission.ACCESS_FINE_LOCATION"),
            WifiPermissionPolicy.requiredRuntimePermissionsForSdk(32)
        )
        assertEquals(
            listOf(
                "android.permission.NEARBY_WIFI_DEVICES",
                "android.permission.ACCESS_FINE_LOCATION",
            ),
            WifiPermissionPolicy.requiredRuntimePermissionsForSdk(33)
        )
    }
}
