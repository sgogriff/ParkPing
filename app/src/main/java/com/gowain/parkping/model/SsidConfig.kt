package com.gowain.parkping.model

data class SsidConfig(
    val ssidId: String,
    val placeId: String,
    val name: String,
    val ssid: String,
    val enabled: Boolean = true,
) {
    val trimmedSsid: String
        get() = ssid.trim()
}
