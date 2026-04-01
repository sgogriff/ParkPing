package com.gowain.parkping.model

data class PlaceConfig(
    val placeId: String,
    val name: String,
    val enabled: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val triggerBehavior: PlaceTriggerBehavior = PlaceTriggerBehavior(),
    val ssids: List<SsidConfig> = emptyList(),
) {
    val hasGeofence: Boolean
        get() = latitude != null && longitude != null

    val enabledSsids: List<SsidConfig>
        get() = ssids.filter { it.enabled && it.trimmedSsid.isNotBlank() }

    companion object {
        const val DEFAULT_RADIUS_METERS = 200f
    }
}
