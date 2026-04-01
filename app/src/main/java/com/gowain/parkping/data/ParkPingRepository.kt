package com.gowain.parkping.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.gowain.parkping.model.DailyReminderState
import com.gowain.parkping.model.MonitoringStatus
import com.gowain.parkping.model.OnboardingState
import com.gowain.parkping.model.ParkPingSettings
import com.gowain.parkping.model.PlaceConfig
import com.gowain.parkping.model.PlaceTriggerBehavior
import com.gowain.parkping.model.ReminderStatus
import com.gowain.parkping.model.RepositorySnapshot
import com.gowain.parkping.model.SsidConfig
import com.gowain.parkping.model.TriggerSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.parkPingDataStore by preferencesDataStore(name = "park_ping")

@Singleton
class ParkPingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter)
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
        .create()

    val snapshotFlow: Flow<RepositorySnapshot> = context.parkPingDataStore.data.map { preferences ->
        preferences.toSnapshot()
    }
    val settingsFlow: Flow<ParkPingSettings> = snapshotFlow.map { it.settings }
    val dailyReminderFlow: Flow<DailyReminderState> = snapshotFlow.map { it.dailyReminderState }
    val monitoringFlow: Flow<MonitoringStatus> = snapshotFlow.map { it.monitoringStatus }
    val onboardingFlow: Flow<OnboardingState> = snapshotFlow.map { it.onboardingState }

    suspend fun currentSnapshot(): RepositorySnapshot = snapshotFlow.first()

    suspend fun saveSettings(settings: ParkPingSettings) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.settingsJson] = gson.toJson(settings)
        }
    }

    suspend fun setOnboardingState(state: OnboardingState) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.onboardingJson] = gson.toJson(state)
        }
    }

    suspend fun updateOnboardingState(transform: (OnboardingState) -> OnboardingState) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.onboardingJson] = gson.toJson(transform(preferences.toOnboardingState()))
        }
    }

    suspend fun updateDailyReminder(transform: (DailyReminderState) -> DailyReminderState) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.dailyReminderJson] = gson.toJson(transform(preferences.toDailyReminderState()))
        }
    }

    suspend fun updateMonitoringStatus(transform: (MonitoringStatus) -> MonitoringStatus) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.monitoringJson] = gson.toJson(transform(preferences.toMonitoringStatus()))
        }
    }

    suspend fun setDailyReminderState(state: DailyReminderState) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.dailyReminderJson] = gson.toJson(state)
        }
    }

    suspend fun setMonitoringStatus(status: MonitoringStatus) {
        context.parkPingDataStore.edit { preferences ->
            preferences[Keys.monitoringJson] = gson.toJson(status)
        }
    }

    private fun Preferences.toSnapshot(): RepositorySnapshot {
        return RepositorySnapshot(
            settings = toSettings(),
            dailyReminderState = toDailyReminderState(),
            monitoringStatus = toMonitoringStatus(),
            onboardingState = toOnboardingState(),
        )
    }

    private fun Preferences.toSettings(): ParkPingSettings {
        this[Keys.settingsJson]?.let { json ->
            return gson.fromJson(json, ParkPingSettings::class.java)
        }
        return toLegacySettings()
    }

    private fun Preferences.toDailyReminderState(): DailyReminderState {
        this[Keys.dailyReminderJson]?.let { json ->
            return gson.fromJson(json, DailyReminderState::class.java)
        }
        return DailyReminderState(
            businessDate = this[LegacyKeys.businessDate]?.let(LocalDate::parse),
            reminderStatus = this[LegacyKeys.reminderStatus]?.let(ReminderStatus::valueOf) ?: ReminderStatus.IDLE,
            completedAt = this[LegacyKeys.completedAt]?.let(Instant::ofEpochMilli),
            snoozedUntil = this[LegacyKeys.snoozedUntil]?.let(Instant::ofEpochMilli),
            lastTriggerSource = this[LegacyKeys.lastTriggerSource]?.let(TriggerSource::valueOf),
            lastTriggerAt = this[LegacyKeys.lastTriggerAt]?.let(Instant::ofEpochMilli),
            activeNotification = this[LegacyKeys.activeNotification] ?: false,
        )
    }

    private fun Preferences.toMonitoringStatus(): MonitoringStatus {
        this[Keys.monitoringJson]?.let { json ->
            return gson.fromJson(json, MonitoringStatus::class.java)
        }
        return MonitoringStatus(
            geofenceRegistered = this[LegacyKeys.geofenceRegistered] ?: false,
            wifiMonitoringRegistered = this[LegacyKeys.wifiMonitoringRegistered] ?: false,
            lastRegistrationError = this[LegacyKeys.lastRegistrationError],
            lastGeofenceEventAt = this[LegacyKeys.lastGeofenceEventAt]?.let(Instant::ofEpochMilli),
            lastWifiEventAt = this[LegacyKeys.lastWifiEventAt]?.let(Instant::ofEpochMilli),
            lastNotificationAt = this[LegacyKeys.lastNotificationAt]?.let(Instant::ofEpochMilli),
        )
    }

    private fun Preferences.toOnboardingState(): OnboardingState {
        this[Keys.onboardingJson]?.let { json ->
            return gson.fromJson(json, OnboardingState::class.java)
        }
        return OnboardingState()
    }

    private fun Preferences.toLegacySettings(): ParkPingSettings {
        val latitude = this[LegacyKeys.workLatitude]
        val longitude = this[LegacyKeys.workLongitude]
        val radiusMeters = this[LegacyKeys.radiusMeters] ?: PlaceConfig.DEFAULT_RADIUS_METERS
        val wifiSsid = this[LegacyKeys.workWifiSsid].orEmpty()
        val geofenceEnabled = this[LegacyKeys.geofenceEnabled] ?: true
        val wifiEnabled = this[LegacyKeys.wifiEnabled] ?: false
        val hasLegacyPlace = latitude != null || longitude != null || wifiSsid.isNotBlank()
        val migratedPlaces = if (hasLegacyPlace) {
            val placeId = "migrated-work"
            listOf(
                PlaceConfig(
                    placeId = placeId,
                    name = "Work",
                    enabled = geofenceEnabled || wifiEnabled || hasLegacyPlace,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    triggerBehavior = PlaceTriggerBehavior(),
                    ssids = wifiSsid.takeIf { it.isNotBlank() }?.let {
                        listOf(
                            SsidConfig(
                                ssidId = UUID.nameUUIDFromBytes("$placeId:$it".toByteArray()).toString(),
                                placeId = placeId,
                                name = "Work Wi-Fi",
                                ssid = it,
                                enabled = wifiEnabled || it.isNotBlank(),
                            ),
                        )
                    }.orEmpty(),
                ),
            )
        } else {
            emptyList()
        }
        return ParkPingSettings(
            places = migratedPlaces,
            reminderText = this[LegacyKeys.reminderText] ?: ParkPingSettings.DEFAULT_REMINDER_TEXT,
            snoozeMinutes = this[LegacyKeys.snoozeMinutes] ?: ParkPingSettings.DEFAULT_SNOOZE_MINUTES,
        )
    }

    private object InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
        override fun serialize(src: Instant?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src?.toString())
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant? {
            val value = json.asString
            return value.takeIf { it.isNotBlank() }?.let(Instant::parse)
        }
    }

    private object LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src?.toString())
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate? {
            val value = json.asString
            return value.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        }
    }

    private object Keys {
        val settingsJson = stringPreferencesKey("settings_json")
        val dailyReminderJson = stringPreferencesKey("daily_reminder_json")
        val monitoringJson = stringPreferencesKey("monitoring_json")
        val onboardingJson = stringPreferencesKey("onboarding_json")
    }

    private object LegacyKeys {
        val workLatitude = doublePreferencesKey("work_latitude")
        val workLongitude = doublePreferencesKey("work_longitude")
        val radiusMeters = floatPreferencesKey("radius_meters")
        val workWifiSsid = stringPreferencesKey("work_wifi_ssid")
        val geofenceEnabled = booleanPreferencesKey("geofence_enabled")
        val wifiEnabled = booleanPreferencesKey("wifi_enabled")
        val reminderText = stringPreferencesKey("reminder_text")
        val snoozeMinutes = intPreferencesKey("snooze_minutes")

        val businessDate = stringPreferencesKey("business_date")
        val reminderStatus = stringPreferencesKey("reminder_status")
        val completedAt = longPreferencesKey("completed_at")
        val snoozedUntil = longPreferencesKey("snoozed_until")
        val lastTriggerSource = stringPreferencesKey("last_trigger_source")
        val lastTriggerAt = longPreferencesKey("last_trigger_at")
        val activeNotification = booleanPreferencesKey("active_notification")

        val geofenceRegistered = booleanPreferencesKey("geofence_registered")
        val wifiMonitoringRegistered = booleanPreferencesKey("wifi_monitoring_registered")
        val lastRegistrationError = stringPreferencesKey("last_registration_error")
        val lastGeofenceEventAt = longPreferencesKey("last_geofence_event_at")
        val lastWifiEventAt = longPreferencesKey("last_wifi_event_at")
        val lastNotificationAt = longPreferencesKey("last_notification_at")
    }
}
