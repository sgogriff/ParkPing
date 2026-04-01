package com.gowain.parkping.ui

import com.gowain.parkping.model.AppLanguage
import com.gowain.parkping.model.ParkPingSettings
import com.gowain.parkping.model.PlaceConfig
import com.gowain.parkping.model.PlaceTriggerBehavior
import com.gowain.parkping.model.SsidConfig
import java.util.UUID

data class SetupDraft(
    val places: List<PlaceDraft> = emptyList(),
    val reminderText: String = ParkPingSettings.DEFAULT_REMINDER_TEXT,
    val snoozeMinutesText: String = ParkPingSettings.DEFAULT_SNOOZE_MINUTES.toString(),
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedPlaceId: String? = null,
) {
    val selectedPlace: PlaceDraft?
        get() = places.firstOrNull { it.placeId == selectedPlaceId } ?: places.firstOrNull()

    companion object {
        fun fromSettings(settings: ParkPingSettings): SetupDraft {
            val places = settings.places.map(PlaceDraft::fromConfig)
            return SetupDraft(
                places = places,
                reminderText = settings.reminderText,
                snoozeMinutesText = settings.snoozeMinutes.toString(),
                language = settings.language,
                selectedPlaceId = places.firstOrNull()?.placeId,
            )
        }

        fun newPlace(
            name: String = "Work",
            ssidName: String = "Wi-Fi",
        ): PlaceDraft {
            val placeId = UUID.randomUUID().toString()
            return PlaceDraft(
                placeId = placeId,
                name = name,
                ssids = listOf(newSsid(placeId, ssidName)),
            )
        }

        fun newSsid(
            placeId: String,
            name: String = "Wi-Fi",
        ): SsidDraft {
            return SsidDraft(
                ssidId = UUID.randomUUID().toString(),
                placeId = placeId,
                name = name,
            )
        }
    }
}

data class PlaceDraft(
    val placeId: String,
    val name: String = "",
    val enabled: Boolean = true,
    val latitudeText: String = "",
    val longitudeText: String = "",
    val radiusText: String = PlaceConfig.DEFAULT_RADIUS_METERS.toInt().toString(),
    val triggerOnPlaceSwitch: Boolean = false,
    val returnAfterAwayEnabled: Boolean = false,
    val returnAfterAwayMinutesText: String = "",
    val ssids: List<SsidDraft> = emptyList(),
) {
    companion object {
        fun fromConfig(config: PlaceConfig): PlaceDraft {
            return PlaceDraft(
                placeId = config.placeId,
                name = config.name,
                enabled = config.enabled,
                latitudeText = config.latitude?.toString().orEmpty(),
                longitudeText = config.longitude?.toString().orEmpty(),
                radiusText = config.radiusMeters.toInt().toString(),
                triggerOnPlaceSwitch = config.triggerBehavior.triggerOnPlaceSwitch,
                returnAfterAwayEnabled = config.triggerBehavior.returnAfterAwayMinutes != null,
                returnAfterAwayMinutesText = config.triggerBehavior.returnAfterAwayMinutes?.toString().orEmpty(),
                ssids = config.ssids.map(SsidDraft::fromConfig),
            )
        }
    }
}

data class SsidDraft(
    val ssidId: String,
    val placeId: String,
    val name: String = "",
    val ssid: String = "",
    val enabled: Boolean = true,
) {
    companion object {
        fun fromConfig(config: SsidConfig): SsidDraft {
            return SsidDraft(
                ssidId = config.ssidId,
                placeId = config.placeId,
                name = config.name,
                ssid = config.ssid,
                enabled = config.enabled,
            )
        }
    }
}

fun SetupDraft.toSettings(): ParkPingSettings {
    return ParkPingSettings(
        places = places.mapNotNull { draft ->
            val radiusMeters = draft.radiusText.toFloatOrNull() ?: return@mapNotNull null
            val latitude = draft.latitudeText.toDoubleOrNull()
            val longitude = draft.longitudeText.toDoubleOrNull()
            PlaceConfig(
                placeId = draft.placeId,
                name = draft.name.trim(),
                enabled = draft.enabled,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                triggerBehavior = PlaceTriggerBehavior(
                    triggerOnPlaceSwitch = draft.triggerOnPlaceSwitch,
                    returnAfterAwayMinutes = if (draft.returnAfterAwayEnabled) {
                        draft.returnAfterAwayMinutesText.toIntOrNull()
                    } else {
                        null
                    },
                ),
                ssids = draft.ssids.map { ssid ->
                    SsidConfig(
                        ssidId = ssid.ssidId,
                        placeId = draft.placeId,
                        name = ssid.name.trim(),
                        ssid = ssid.ssid,
                        enabled = ssid.enabled,
                    )
                },
            )
        },
        reminderText = reminderText,
        snoozeMinutes = snoozeMinutesText.toIntOrNull() ?: ParkPingSettings.DEFAULT_SNOOZE_MINUTES,
        language = language,
    )
}
