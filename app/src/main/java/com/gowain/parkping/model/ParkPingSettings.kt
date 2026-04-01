package com.gowain.parkping.model

data class ParkPingSettings(
    val places: List<PlaceConfig> = emptyList(),
    val reminderText: String = DEFAULT_REMINDER_TEXT,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val language: AppLanguage = AppLanguage.ENGLISH,
) {
    val enabledPlaces: List<PlaceConfig>
        get() = places.filter { it.enabled }

    val enabledGeofencePlaces: List<PlaceConfig>
        get() = enabledPlaces.filter { it.hasGeofence }

    val enabledWifiPlaces: List<PlaceConfig>
        get() = enabledPlaces.filter { it.enabledSsids.isNotEmpty() }

    fun placeById(placeId: String?): PlaceConfig? = places.firstOrNull { it.placeId == placeId }

    fun placeName(placeId: String?): String? = placeById(placeId)?.name

    fun findPlaceForSsid(rawSsid: String?): PlaceConfig? {
        val normalized = rawSsid
            ?.removePrefix("\"")
            ?.removeSuffix("\"")
            ?.trim()
            .orEmpty()
            .lowercase()
        if (normalized.isBlank()) {
            return null
        }
        return enabledPlaces.firstOrNull { place ->
            place.enabledSsids.any { it.trimmedSsid.lowercase() == normalized }
        }
    }

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val DEFAULT_REMINDER_TEXT = "Have you registered parking today?"
    }
}
