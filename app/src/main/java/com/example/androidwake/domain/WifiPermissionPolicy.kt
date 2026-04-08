package com.example.androidwake.domain

object WifiPermissionPolicy {
    fun requiredRuntimePermissionsForSdk(sdkInt: Int): List<String> {
        val permissions = mutableListOf<String>()
        if (sdkInt >= 33) {
            permissions += "android.permission.NEARBY_WIFI_DEVICES"
        }
        if (sdkInt >= 23) {
            permissions += "android.permission.ACCESS_FINE_LOCATION"
        }
        return permissions
    }
}
