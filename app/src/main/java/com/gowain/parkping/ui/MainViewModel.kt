package com.gowain.parkping.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowain.parkping.R
import com.gowain.parkping.data.ParkPingRepository
import com.gowain.parkping.location.CurrentLocationProvider
import com.gowain.parkping.model.AppLanguage
import com.gowain.parkping.model.DailyReminderState
import com.gowain.parkping.model.MonitoringStatus
import com.gowain.parkping.model.OnboardingState
import com.gowain.parkping.model.ParkPingSettings
import com.gowain.parkping.model.PermissionSnapshot
import com.gowain.parkping.model.PlaceConfig
import com.gowain.parkping.model.PlaceTriggerBehavior
import com.gowain.parkping.model.RepositorySnapshot
import com.gowain.parkping.model.SsidConfig
import com.gowain.parkping.model.TriggerSource
import com.gowain.parkping.monitor.MonitoringCoordinator
import com.gowain.parkping.monitor.PermissionManager
import com.gowain.parkping.monitor.TriggerProcessor
import com.gowain.parkping.util.BusinessDayCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ParkPingRepository,
    private val permissionManager: PermissionManager,
    private val monitoringCoordinator: MonitoringCoordinator,
    private val triggerProcessor: TriggerProcessor,
    private val currentLocationProvider: CurrentLocationProvider,
    private val businessDayCalculator: BusinessDayCalculator,
    private val appLanguageManager: AppLanguageManager,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val permissionState = MutableStateFlow(permissionManager.snapshot())
    private val draftState = MutableStateFlow(SetupDraft())
    private val statusMessageState = MutableStateFlow<String?>(null)
    private val repositoryUiState = combine(
        repository.settingsFlow,
        repository.dailyReminderFlow,
        repository.monitoringFlow,
        repository.onboardingFlow,
    ) { settings: ParkPingSettings, dailyState: DailyReminderState, monitoringStatus: MonitoringStatus, onboardingState: OnboardingState ->
        RepositorySnapshot(
            settings = settings,
            dailyReminderState = dailyState,
            monitoringStatus = monitoringStatus,
            onboardingState = onboardingState,
        )
    }

    val uiState: StateFlow<AppUiState> = combine(
        repositoryUiState,
        permissionState,
        draftState,
        statusMessageState,
    ) { repositoryState: RepositorySnapshot, permissions: PermissionSnapshot, draft: SetupDraft, statusMessage: String? ->
        AppUiState(
            settings = repositoryState.settings,
            dailyReminderState = repositoryState.dailyReminderState,
            monitoringStatus = repositoryState.monitoringStatus,
            permissions = permissions,
            setupDraft = draft,
            onboardingState = repositoryState.onboardingState,
            currentBusinessDate = businessDayCalculator.currentBusinessDate(),
            statusMessage = statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        viewModelScope.launch {
            repository.snapshotFlow.collect { snapshot ->
                draftState.value = SetupDraft.fromSettings(snapshot.settings).copy(
                    language = appLanguageManager.currentLanguage(),
                )
            }
        }
        refreshRuntimeState()
    }

    fun updateDraft(newDraft: SetupDraft) {
        draftState.value = newDraft
    }

    fun clearStatusMessage() {
        statusMessageState.value = null
    }

    fun refreshRuntimeState() {
        permissionState.value = permissionManager.snapshot()
        viewModelScope.launch {
            monitoringCoordinator.syncMonitoring()
        }
    }

    fun syncMonitoring() {
        viewModelScope.launch {
            monitoringCoordinator.syncMonitoring()
            permissionState.value = permissionManager.snapshot()
            statusMessageState.value = appContext.getString(R.string.status_monitoring_refreshed)
        }
    }

    fun updateReminderText(value: String) {
        draftState.update { it.copy(reminderText = value) }
    }

    fun updateSnoozeMinutes(value: String) {
        draftState.update { it.copy(snoozeMinutesText = value) }
    }

    fun updateLanguage(language: AppLanguage) {
        draftState.update { it.copy(language = language) }
        appLanguageManager.persistAndApply(language)
    }

    fun selectPlace(placeId: String) {
        draftState.update { it.copy(selectedPlaceId = placeId) }
    }

    fun addPlace() {
        val place = SetupDraft.newPlace(
            name = appContext.getString(R.string.default_place_name),
            ssidName = appContext.getString(R.string.default_ssid_name),
        )
        draftState.update { current ->
            current.copy(
                places = current.places + place,
                selectedPlaceId = place.placeId,
            )
        }
    }

    fun deletePlace(placeId: String) {
        draftState.update { current ->
            val updatedPlaces = current.places.filterNot { it.placeId == placeId }
            current.copy(
                places = updatedPlaces,
                selectedPlaceId = when {
                    current.selectedPlaceId != placeId -> current.selectedPlaceId
                    updatedPlaces.isNotEmpty() -> updatedPlaces.first().placeId
                    else -> null
                },
            )
        }
    }

    fun updatePlace(placeId: String, transform: (PlaceDraft) -> PlaceDraft) {
        draftState.update { current ->
            current.copy(
                places = current.places.map { place ->
                    if (place.placeId == placeId) transform(place) else place
                },
            )
        }
    }

    fun addSsid(placeId: String) {
        updatePlace(placeId) { place ->
            place.copy(
                ssids = place.ssids + SetupDraft.newSsid(
                    placeId = placeId,
                    name = appContext.getString(R.string.default_ssid_name),
                ),
            )
        }
    }

    fun deleteSsid(placeId: String, ssidId: String) {
        updatePlace(placeId) { place ->
            place.copy(ssids = place.ssids.filterNot { it.ssidId == ssidId })
        }
    }

    fun updateSsid(placeId: String, ssidId: String, transform: (SsidDraft) -> SsidDraft) {
        updatePlace(placeId) { place ->
            place.copy(
                ssids = place.ssids.map { ssid ->
                    if (ssid.ssidId == ssidId) transform(ssid) else ssid
                },
            )
        }
    }

    fun saveSettings(completeOnboarding: Boolean = false) {
        val draft = draftState.value
        val validation = validateDraft(draft)
        val settings = validation.settings
        if (settings == null) {
            statusMessageState.value = validation.message
            return
        }
        viewModelScope.launch {
            repository.saveSettings(settings)
            if (completeOnboarding) {
                repository.setOnboardingState(
                    OnboardingState(
                        privacyAcknowledged = true,
                        completed = true,
                    ),
                )
            }
            monitoringCoordinator.syncMonitoring()
            permissionState.value = permissionManager.snapshot()
            statusMessageState.value = if (completeOnboarding) {
                appContext.getString(R.string.status_onboarding_complete)
            } else {
                appContext.getString(R.string.status_settings_saved)
            }
        }
    }

    fun markDone() {
        viewModelScope.launch {
            triggerProcessor.markDone()
            statusMessageState.value = appContext.getString(
                R.string.status_marked_done,
                businessDayCalculator.currentBusinessDate(),
            )
        }
    }

    fun snooze() {
        viewModelScope.launch {
            triggerProcessor.snooze()
            statusMessageState.value = appContext.getString(R.string.status_reminder_snoozed)
        }
    }

    fun rearm() {
        viewModelScope.launch {
            triggerProcessor.rearm()
            monitoringCoordinator.syncMonitoring()
            permissionState.value = permissionManager.snapshot()
            statusMessageState.value = appContext.getString(R.string.status_rearmed)
        }
    }

    fun testReminder() {
        viewModelScope.launch {
            val shown = triggerProcessor.previewReminder()
            statusMessageState.value = if (shown) {
                appContext.getString(R.string.status_test_reminder_shown)
            } else {
                appContext.getString(R.string.error_test_reminder_unavailable)
            }
        }
    }

    fun acknowledgePrivacyStep() {
        viewModelScope.launch {
            repository.updateOnboardingState { current ->
                current.copy(privacyAcknowledged = true)
            }
        }
    }

    fun finishOnboardingWithoutSetup() {
        viewModelScope.launch {
            repository.saveSettings(
                repository.currentSnapshot().settings.copy(language = draftState.value.language),
            )
            repository.setOnboardingState(
                OnboardingState(
                    privacyAcknowledged = true,
                    completed = true,
                ),
            )
            monitoringCoordinator.syncMonitoring()
            permissionState.value = permissionManager.snapshot()
            statusMessageState.value = appContext.getString(R.string.status_onboarding_complete)
        }
    }

    fun useCurrentLocation(placeId: String?) {
        val targetPlaceId = placeId ?: draftState.value.selectedPlaceId
        if (targetPlaceId == null) {
            statusMessageState.value = appContext.getString(R.string.error_select_place)
            return
        }
        if (!permissionManager.snapshot().fineLocationGranted) {
            statusMessageState.value = appContext.getString(R.string.error_location_permission_required)
            return
        }
        viewModelScope.launch {
            val currentLocation = currentLocationProvider.currentLocation()
            if (currentLocation == null) {
                statusMessageState.value = appContext.getString(R.string.error_current_location_unavailable)
                return@launch
            }
            updatePlace(targetPlaceId) { place ->
                place.copy(
                    latitudeText = "%.6f".format(currentLocation.latitude),
                    longitudeText = "%.6f".format(currentLocation.longitude),
                )
            }
            statusMessageState.value = appContext.getString(R.string.status_current_location_copied)
        }
    }

    private fun validateDraft(draft: SetupDraft): DraftValidation {
        val snoozeMinutes = draft.snoozeMinutesText.toIntOrNull()
        if (snoozeMinutes == null || snoozeMinutes <= 0) {
            return DraftValidation(message = appContext.getString(R.string.error_invalid_snooze))
        }
        val seenSsids = mutableSetOf<String>()
        val places = mutableListOf<PlaceConfig>()
        draft.places.forEach { place ->
            val trimmedName = place.name.trim()
            val hasCoordinateInput = place.latitudeText.isNotBlank() || place.longitudeText.isNotBlank()
            val latitude = place.latitudeText.toDoubleOrNull()
            val longitude = place.longitudeText.toDoubleOrNull()
            if (hasCoordinateInput && (latitude == null || longitude == null)) {
                return DraftValidation(message = appContext.getString(R.string.error_invalid_coordinates))
            }
            val radius = if (place.radiusText.isBlank()) {
                PlaceConfig.DEFAULT_RADIUS_METERS
            } else {
                place.radiusText.toFloatOrNull()
            }
            if (hasCoordinateInput && (radius == null || radius <= 0f)) {
                return DraftValidation(message = appContext.getString(R.string.error_invalid_radius))
            }
            val returnAfterAwayMinutes = if (place.returnAfterAwayEnabled) {
                val minutes = place.returnAfterAwayMinutesText.toIntOrNull()
                if (minutes == null || minutes <= 0) {
                    return DraftValidation(message = appContext.getString(R.string.error_invalid_return_time))
                }
                minutes
            } else {
                null
            }
            val ssids = mutableListOf<SsidConfig>()
            place.ssids.forEach { ssid ->
                val rawSsid = ssid.ssid.trim()
                if (rawSsid.isBlank() && ssid.name.isBlank()) {
                    return@forEach
                }
                if (rawSsid.isBlank()) {
                    return DraftValidation(message = appContext.getString(R.string.error_invalid_ssid))
                }
                val normalizedSsid = rawSsid.lowercase()
                if (!seenSsids.add(normalizedSsid)) {
                    return DraftValidation(message = appContext.getString(R.string.error_duplicate_ssids))
                }
                ssids += SsidConfig(
                    ssidId = ssid.ssidId,
                    placeId = place.placeId,
                    name = ssid.name.trim().ifBlank {
                        appContext.getString(R.string.default_ssid_name)
                    },
                    ssid = rawSsid,
                    enabled = ssid.enabled,
                )
            }
            val hasAnyConfig = trimmedName.isNotBlank() || hasCoordinateInput || ssids.isNotEmpty()
            if (!hasAnyConfig) {
                return@forEach
            }
            if (trimmedName.isBlank()) {
                return DraftValidation(message = appContext.getString(R.string.error_place_name_required))
            }
            places += PlaceConfig(
                placeId = place.placeId,
                name = trimmedName,
                enabled = place.enabled,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radius ?: PlaceConfig.DEFAULT_RADIUS_METERS,
                triggerBehavior = PlaceTriggerBehavior(
                    triggerOnPlaceSwitch = place.triggerOnPlaceSwitch,
                    returnAfterAwayMinutes = returnAfterAwayMinutes,
                ),
                ssids = ssids,
            )
        }
        return DraftValidation(
            settings = ParkPingSettings(
                places = places,
                reminderText = draft.reminderText.ifBlank {
                    ParkPingSettings.DEFAULT_REMINDER_TEXT
                },
                snoozeMinutes = snoozeMinutes,
                language = draft.language,
            ),
        )
    }

    private data class DraftValidation(
        val settings: ParkPingSettings? = null,
        val message: String? = null,
    )
}
