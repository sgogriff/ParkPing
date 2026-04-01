package com.gowain.parkping.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gowain.parkping.R
import com.gowain.parkping.model.AppLanguage
import com.gowain.parkping.model.OnboardingState
import com.gowain.parkping.model.ReminderStatus
import com.gowain.parkping.model.TriggerSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class AppScreen {
    HOME,
    TODAY,
    SETUP,
}

private enum class OnboardingStep {
    WHAT,
    PRIVACY,
    PERMISSIONS,
    FIRST_PLACE,
}

private val instantFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkPingApp(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var onboardingStep by rememberSaveable(
        uiState.onboardingState.privacyAcknowledged,
        uiState.onboardingState.completed,
    ) {
        mutableStateOf(initialOnboardingStep(uiState.onboardingState))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshRuntimeState() }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshRuntimeState() }
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshRuntimeState() }

    fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openAppSettings()
        } else {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshRuntimeState()
    }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearStatusMessage()
    }

    LaunchedEffect(onboardingStep, uiState.setupDraft.places.isEmpty(), uiState.onboardingState.completed) {
        if (
            !uiState.onboardingState.completed &&
            onboardingStep == OnboardingStep.FIRST_PLACE &&
            uiState.setupDraft.places.isEmpty()
        ) {
            viewModel.addPlace()
        }
    }

    val backgroundBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF08131E),
                Color(0xFF10263B),
                Color(0xFF173A56),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        if (!uiState.onboardingState.completed) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = stringResource(R.string.onboarding_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                when (onboardingStep) {
                    OnboardingStep.WHAT -> OnboardingIntroScreen(
                        contentPadding = innerPadding,
                        onContinue = { onboardingStep = OnboardingStep.PRIVACY },
                    )

                    OnboardingStep.PRIVACY -> OnboardingPrivacyScreen(
                        uiState = uiState,
                        contentPadding = innerPadding,
                        onLanguageSelected = viewModel::updateLanguage,
                        onContinue = {
                            viewModel.acknowledgePrivacyStep()
                            onboardingStep = OnboardingStep.PERMISSIONS
                        },
                    )

                    OnboardingStep.PERMISSIONS -> OnboardingPermissionsScreen(
                        uiState = uiState,
                        contentPadding = innerPadding,
                        onBack = { onboardingStep = OnboardingStep.PRIVACY },
                        onContinue = { onboardingStep = OnboardingStep.FIRST_PLACE },
                        onRequestNotifications = ::requestNotificationPermission,
                        onRequestLocation = ::requestLocationPermission,
                        onRequestBackgroundLocation = ::requestBackgroundLocation,
                    )

                    OnboardingStep.FIRST_PLACE -> OnboardingPlaceScreen(
                        uiState = uiState,
                        contentPadding = innerPadding,
                        onBack = { onboardingStep = OnboardingStep.PERMISSIONS },
                        onSkip = viewModel::finishOnboardingWithoutSetup,
                        onSaveAndFinish = { viewModel.saveSettings(completeOnboarding = true) },
                        onSelectPlace = viewModel::selectPlace,
                        onAddPlace = viewModel::addPlace,
                        onDeletePlace = viewModel::deletePlace,
                        onUpdatePlace = viewModel::updatePlace,
                        onAddSsid = viewModel::addSsid,
                        onDeleteSsid = viewModel::deleteSsid,
                        onUpdateSsid = viewModel::updateSsid,
                        onUseCurrentLocation = viewModel::useCurrentLocation,
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = stringResource(R.string.business_day_label, uiState.currentBusinessDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        NavigationBarItem(
                            selected = selectedScreen == AppScreen.HOME,
                            onClick = { selectedScreen = AppScreen.HOME },
                            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_home)) },
                        )
                        NavigationBarItem(
                            selected = selectedScreen == AppScreen.TODAY,
                            onClick = { selectedScreen = AppScreen.TODAY },
                            icon = { Icon(Icons.Default.Today, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_today)) },
                        )
                        NavigationBarItem(
                            selected = selectedScreen == AppScreen.SETUP,
                            onClick = { selectedScreen = AppScreen.SETUP },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_setup)) },
                        )
                    }
                },
            ) { innerPadding ->
                when (selectedScreen) {
                    AppScreen.HOME -> HomeScreen(
                        uiState = uiState,
                        contentPadding = innerPadding,
                        onMarkDone = viewModel::markDone,
                        onSnooze = viewModel::snooze,
                        onRearm = viewModel::rearm,
                        onSync = viewModel::syncMonitoring,
                        onTestReminder = viewModel::testReminder,
                        onOpenSetup = { selectedScreen = AppScreen.SETUP },
                    )

                    AppScreen.TODAY -> TodayScreen(
                        uiState = uiState,
                        contentPadding = innerPadding,
                        onOpenSetup = { selectedScreen = AppScreen.SETUP },
                    )

                    AppScreen.SETUP -> SetupScreen(
                        uiState = uiState,
                        contentPadding = innerPadding,
                        onUpdateReminderText = viewModel::updateReminderText,
                        onUpdateSnoozeMinutes = viewModel::updateSnoozeMinutes,
                        onLanguageSelected = viewModel::updateLanguage,
                        onSelectPlace = viewModel::selectPlace,
                        onAddPlace = viewModel::addPlace,
                        onDeletePlace = viewModel::deletePlace,
                        onUpdatePlace = viewModel::updatePlace,
                        onAddSsid = viewModel::addSsid,
                        onDeleteSsid = viewModel::deleteSsid,
                        onUpdateSsid = viewModel::updateSsid,
                        onUseCurrentLocation = viewModel::useCurrentLocation,
                        onRequestNotifications = ::requestNotificationPermission,
                        onRequestLocation = ::requestLocationPermission,
                        onRequestBackgroundLocation = ::requestBackgroundLocation,
                        onSave = { viewModel.saveSettings() },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPrivacyScreen(
    uiState: AppUiState,
    contentPadding: PaddingValues,
    onLanguageSelected: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
) {
    ScreenColumn(contentPadding) {
        HeroCard(
            title = stringResource(R.string.onboarding_privacy_title),
            subtitle = stringResource(R.string.onboarding_privacy_subtitle),
        ) {
            LanguageSelector(
                selected = uiState.setupDraft.language,
                onSelected = onLanguageSelected,
            )
        }

        InfoCard(title = stringResource(R.string.onboarding_local_title)) {
            Text(
                text = stringResource(R.string.onboarding_local_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        InfoCard(title = stringResource(R.string.onboarding_map_title)) {
            Text(
                text = stringResource(R.string.onboarding_map_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        InfoCard(title = stringResource(R.string.onboarding_delete_title)) {
            Text(
                text = stringResource(R.string.onboarding_delete_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        PrimaryActionRow(
            primaryLabel = stringResource(R.string.action_continue),
            onPrimary = onContinue,
        )
    }
}

@Composable
private fun OnboardingIntroScreen(
    contentPadding: PaddingValues,
    onContinue: () -> Unit,
) {
    ScreenColumn(contentPadding) {
        HeroCard(
            title = stringResource(R.string.onboarding_intro_title),
            subtitle = stringResource(R.string.onboarding_intro_subtitle),
        ) {
            Text(
                text = stringResource(R.string.onboarding_intro_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryActionRow(
            primaryLabel = stringResource(R.string.action_continue),
            onPrimary = onContinue,
        )
    }
}

@Composable
private fun OnboardingPermissionsScreen(
    uiState: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
) {
    ScreenColumn(contentPadding) {
        HeroCard(
            title = stringResource(R.string.onboarding_permissions_title),
            subtitle = stringResource(R.string.onboarding_permissions_subtitle),
        )

        PermissionStatusCard(
            permissions = uiState.permissions,
            monitoringStatus = uiState.monitoringStatus,
            onRequestNotifications = onRequestNotifications,
            onRequestLocation = onRequestLocation,
            onRequestBackgroundLocation = onRequestBackgroundLocation,
        )

        PrimaryActionRow(
            primaryLabel = stringResource(R.string.action_continue),
            onPrimary = onContinue,
            secondaryLabel = stringResource(R.string.action_back),
            onSecondary = onBack,
        )
    }
}

@Composable
private fun OnboardingPlaceScreen(
    uiState: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onSaveAndFinish: () -> Unit,
    onSelectPlace: (String) -> Unit,
    onAddPlace: () -> Unit,
    onDeletePlace: (String) -> Unit,
    onUpdatePlace: (String, (PlaceDraft) -> PlaceDraft) -> Unit,
    onAddSsid: (String) -> Unit,
    onDeleteSsid: (String, String) -> Unit,
    onUpdateSsid: (String, String, (SsidDraft) -> SsidDraft) -> Unit,
    onUseCurrentLocation: (String?) -> Unit,
) {
    ScreenColumn(contentPadding) {
        HeroCard(
            title = stringResource(R.string.onboarding_first_place_title),
            subtitle = stringResource(R.string.onboarding_first_place_subtitle),
        )

        TriggerTermsCard()

        if (uiState.setupDraft.places.isEmpty()) {
            EmptyStateCard(
                title = stringResource(R.string.setup_places_empty_title),
                body = stringResource(R.string.setup_places_empty_body),
                actionLabel = stringResource(R.string.action_add_place),
                onAction = onAddPlace,
            )
        } else {
            PlaceCollectionSection(
                draft = uiState.setupDraft,
                onSelectPlace = onSelectPlace,
                onAddPlace = onAddPlace,
                onDeletePlace = onDeletePlace,
            )
            uiState.setupDraft.selectedPlace?.let { place ->
                PlaceEditorCard(
                    place = place,
                    onUpdatePlace = onUpdatePlace,
                    onAddSsid = onAddSsid,
                    onDeleteSsid = onDeleteSsid,
                    onUpdateSsid = onUpdateSsid,
                    onUseCurrentLocation = onUseCurrentLocation,
                )
            }
        }

        PrimaryActionRow(
            primaryLabel = stringResource(R.string.action_finish_setup),
            onPrimary = onSaveAndFinish,
            secondaryLabel = stringResource(R.string.action_skip_for_now),
            onSecondary = onSkip,
            tertiaryLabel = stringResource(R.string.action_back),
            onTertiary = onBack,
        )
    }
}

@Composable
private fun HomeScreen(
    uiState: AppUiState,
    contentPadding: PaddingValues,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onRearm: () -> Unit,
    onSync: () -> Unit,
    onTestReminder: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    val reminderState = uiState.dailyReminderState
    val monitoringOn = uiState.monitoringStatus.geofenceRegistered || uiState.monitoringStatus.wifiMonitoringRegistered
    val armed = monitoringOn && reminderState.reminderStatus == ReminderStatus.IDLE

    ScreenColumn(contentPadding) {
        HeroCard(
            title = if (armed) {
                stringResource(R.string.home_armed_title)
            } else {
                stringResource(R.string.home_unarmed_title)
            },
            subtitle = if (armed) {
                stringResource(R.string.home_armed_subtitle)
            } else {
                when {
                    uiState.settings.places.none { it.hasGeofence || it.enabledSsids.isNotEmpty() } -> stringResource(R.string.home_unarmed_setup)
                    reminderState.reminderStatus == ReminderStatus.ACTIVE -> stringResource(R.string.home_unarmed_active)
                    reminderState.reminderStatus == ReminderStatus.SNOOZED -> stringResource(R.string.home_unarmed_snoozed)
                    reminderState.reminderStatus == ReminderStatus.COMPLETED -> stringResource(R.string.home_unarmed_completed)
                    else -> stringResource(R.string.home_unarmed_idle)
                }
            },
        ) {
            ArmStateBadge(armed = armed)
        }

        InfoCard(title = stringResource(R.string.home_actions_title)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onMarkDone,
                    enabled = reminderState.reminderStatus != ReminderStatus.COMPLETED,
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_mark_done))
                }
                FilledTonalButton(
                    onClick = onSnooze,
                    enabled = reminderState.reminderStatus == ReminderStatus.ACTIVE,
                ) {
                    Text(stringResource(R.string.action_snooze))
                }
                if (reminderState.reminderStatus == ReminderStatus.COMPLETED) {
                    OutlinedButton(onClick = onRearm) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_rearm))
                    }
                }
                OutlinedButton(onClick = onTestReminder) {
                    Text(stringResource(R.string.action_test_reminder))
                }
                OutlinedButton(onClick = onSync) {
                    Text(stringResource(R.string.action_sync_monitoring))
                }
                OutlinedButton(onClick = onOpenSetup) {
                    Text(stringResource(R.string.action_open_setup))
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    uiState: AppUiState,
    contentPadding: PaddingValues,
    onOpenSetup: () -> Unit,
) {
    val reminderState = uiState.dailyReminderState
    val placeName = uiState.settings.placeName(reminderState.lastTriggerPlaceId)
    val setupIncomplete = uiState.settings.places.none { it.hasGeofence || it.enabledSsids.isNotEmpty() }
    val requiresBackgroundLocation = uiState.settings.enabledGeofencePlaces.isNotEmpty() && !uiState.permissions.backgroundLocationGranted
    val requiresLocation = uiState.settings.enabledWifiPlaces.isNotEmpty() && !uiState.permissions.fineLocationGranted

    ScreenColumn(contentPadding) {
        HeroCard(
            title = when (reminderState.reminderStatus) {
                ReminderStatus.ACTIVE -> stringResource(R.string.today_status_active)
                ReminderStatus.SNOOZED -> stringResource(R.string.today_status_snoozed)
                ReminderStatus.COMPLETED -> stringResource(R.string.today_status_completed)
                ReminderStatus.IDLE -> stringResource(R.string.today_status_idle)
            },
            subtitle = placeName?.let {
                stringResource(R.string.today_last_place_subtitle, it)
            } ?: uiState.settings.reminderText,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryChip(stringResource(R.string.summary_places, uiState.settings.places.size))
            SummaryChip(
                if (uiState.monitoringStatus.geofenceRegistered || uiState.monitoringStatus.wifiMonitoringRegistered) {
                    stringResource(R.string.summary_monitoring_ready)
                } else {
                    stringResource(R.string.summary_monitoring_needs_setup)
                },
            )
            SummaryChip(
                when (reminderState.reminderStatus) {
                    ReminderStatus.COMPLETED -> stringResource(R.string.summary_done)
                    ReminderStatus.ACTIVE -> stringResource(R.string.summary_armed)
                    ReminderStatus.SNOOZED -> stringResource(R.string.summary_snoozed)
                    ReminderStatus.IDLE -> stringResource(R.string.summary_waiting)
                },
            )
        }

        if (setupIncomplete || requiresBackgroundLocation || requiresLocation) {
            InfoCard(title = stringResource(R.string.today_setup_needed_title)) {
                Text(
                    text = when {
                        setupIncomplete -> stringResource(R.string.today_setup_needed_body)
                        requiresBackgroundLocation -> stringResource(R.string.today_background_needed_body)
                        else -> stringResource(R.string.today_location_needed_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenSetup) {
                    Text(stringResource(R.string.action_open_setup))
                }
            }
        }

        InfoCard(title = stringResource(R.string.today_monitoring_title)) {
            StatusRow(
                label = stringResource(R.string.monitoring_geofence),
                value = if (uiState.monitoringStatus.geofenceRegistered) {
                    stringResource(R.string.status_on)
                } else {
                    stringResource(R.string.status_off)
                },
            )
            StatusRow(
                label = stringResource(R.string.monitoring_wifi),
                value = if (uiState.monitoringStatus.wifiMonitoringRegistered) {
                    stringResource(R.string.status_on)
                } else {
                    stringResource(R.string.status_off)
                },
            )
            DetailRow(
                label = stringResource(R.string.monitoring_last_error),
                value = uiState.monitoringStatus.lastRegistrationError ?: stringResource(R.string.value_none),
            )
        }

        InfoCard(title = stringResource(R.string.today_details_title)) {
            DetailRow(
                label = stringResource(R.string.today_detail_status),
                value = localizedReminderStatus(reminderState.reminderStatus),
            )
            DetailRow(
                label = stringResource(R.string.today_detail_source),
                value = localizedTriggerSource(reminderState.lastTriggerSource),
            )
            DetailRow(
                label = stringResource(R.string.today_detail_place),
                value = placeName ?: stringResource(R.string.value_none),
            )
            DetailRow(
                label = stringResource(R.string.today_detail_last_trigger),
                value = formatInstant(reminderState.lastTriggerAt),
            )
            DetailRow(
                label = stringResource(R.string.today_detail_completed),
                value = formatInstant(reminderState.completedAt),
            )
            DetailRow(
                label = stringResource(R.string.today_detail_snoozed_until),
                value = formatInstant(reminderState.snoozedUntil),
            )
        }
    }
}

@Composable
private fun SetupScreen(
    uiState: AppUiState,
    contentPadding: PaddingValues,
    onUpdateReminderText: (String) -> Unit,
    onUpdateSnoozeMinutes: (String) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSelectPlace: (String) -> Unit,
    onAddPlace: () -> Unit,
    onDeletePlace: (String) -> Unit,
    onUpdatePlace: (String, (PlaceDraft) -> PlaceDraft) -> Unit,
    onAddSsid: (String) -> Unit,
    onDeleteSsid: (String, String) -> Unit,
    onUpdateSsid: (String, String, (SsidDraft) -> SsidDraft) -> Unit,
    onUseCurrentLocation: (String?) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onSave: () -> Unit,
) {
    ScreenColumn(contentPadding) {
        InfoCard(title = stringResource(R.string.setup_language_title)) {
            Text(
                text = stringResource(R.string.setup_language_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LanguageSelector(
                selected = uiState.setupDraft.language,
                onSelected = onLanguageSelected,
            )
        }

        InfoCard(title = stringResource(R.string.setup_permissions_title)) {
            PermissionStatusCard(
                permissions = uiState.permissions,
                monitoringStatus = uiState.monitoringStatus,
                onRequestNotifications = onRequestNotifications,
                onRequestLocation = onRequestLocation,
                onRequestBackgroundLocation = onRequestBackgroundLocation,
            )
        }

        InfoCard(title = stringResource(R.string.setup_reminders_title)) {
            OutlinedTextField(
                value = uiState.setupDraft.reminderText,
                onValueChange = onUpdateReminderText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_reminder_text)) },
                supportingText = {
                    Text(stringResource(R.string.field_reminder_support))
                },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.setupDraft.snoozeMinutesText,
                onValueChange = onUpdateSnoozeMinutes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_snooze_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        TriggerTermsCard()

        PlaceCollectionSection(
            draft = uiState.setupDraft,
            onSelectPlace = onSelectPlace,
            onAddPlace = onAddPlace,
            onDeletePlace = onDeletePlace,
        )

        if (uiState.setupDraft.places.isEmpty()) {
            EmptyStateCard(
                title = stringResource(R.string.setup_places_empty_title),
                body = stringResource(R.string.setup_places_empty_body),
                actionLabel = stringResource(R.string.action_add_place),
                onAction = onAddPlace,
            )
        } else {
            uiState.setupDraft.selectedPlace?.let { place ->
                PlaceEditorCard(
                    place = place,
                    onUpdatePlace = onUpdatePlace,
                    onAddSsid = onAddSsid,
                    onDeleteSsid = onDeleteSsid,
                    onUpdateSsid = onUpdateSsid,
                    onUseCurrentLocation = onUseCurrentLocation,
                )
            }
        }

        InfoCard(title = stringResource(R.string.setup_monitoring_title)) {
            StatusRow(
                label = stringResource(R.string.monitoring_geofence),
                value = if (uiState.monitoringStatus.geofenceRegistered) {
                    stringResource(R.string.status_on)
                } else {
                    stringResource(R.string.status_off)
                },
            )
            StatusRow(
                label = stringResource(R.string.monitoring_wifi),
                value = if (uiState.monitoringStatus.wifiMonitoringRegistered) {
                    stringResource(R.string.status_on)
                } else {
                    stringResource(R.string.status_off)
                },
            )
            DetailRow(
                label = stringResource(R.string.monitoring_last_error),
                value = uiState.monitoringStatus.lastRegistrationError ?: stringResource(R.string.value_none),
            )
        }

        PrimaryActionRow(
            primaryLabel = stringResource(R.string.action_save_changes),
            onPrimary = onSave,
        )
    }
}

@Composable
private fun TriggerTermsCard() {
    InfoCard(title = stringResource(R.string.trigger_terms_title)) {
        Text(
            text = stringResource(R.string.trigger_geofence_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.trigger_geofence_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.trigger_ssid_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.trigger_ssid_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionStatusCard(
    permissions: com.gowain.parkping.model.PermissionSnapshot,
    monitoringStatus: com.gowain.parkping.model.MonitoringStatus,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
) {
    PermissionRow(
        icon = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        },
        title = stringResource(R.string.permission_notifications_title),
        supporting = stringResource(R.string.permission_notifications_body),
        granted = permissions.notificationsGranted,
        actionLabel = stringResource(R.string.action_request),
        onAction = onRequestNotifications,
    )
    Spacer(Modifier.height(12.dp))
    PermissionRow(
        icon = {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        },
        title = stringResource(R.string.permission_location_title),
        supporting = stringResource(R.string.permission_location_body),
        granted = permissions.fineLocationGranted,
        actionLabel = stringResource(R.string.action_request),
        onAction = onRequestLocation,
    )
    Spacer(Modifier.height(12.dp))
    PermissionRow(
        icon = {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        },
        title = stringResource(R.string.permission_background_title),
        supporting = stringResource(R.string.permission_background_body),
        granted = permissions.backgroundLocationGranted,
        actionLabel = stringResource(R.string.action_open_settings),
        onAction = onRequestBackgroundLocation,
    )
    if (!monitoringStatus.lastRegistrationError.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = monitoringStatus.lastRegistrationError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun PlaceCollectionSection(
    draft: SetupDraft,
    onSelectPlace: (String) -> Unit,
    onAddPlace: () -> Unit,
    onDeletePlace: (String) -> Unit,
) {
    InfoCard(title = stringResource(R.string.setup_places_title)) {
        if (draft.places.isEmpty()) {
            Text(
                text = stringResource(R.string.setup_places_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                draft.places.forEach { place ->
                    PlaceSummaryCard(
                        place = place,
                        selected = draft.selectedPlaceId == place.placeId,
                        onSelect = { onSelectPlace(place.placeId) },
                        onDelete = { onDeletePlace(place.placeId) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onAddPlace) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_add_place))
        }
    }
}

@Composable
private fun PlaceSummaryCard(
    place: PlaceDraft,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = if (selected) {
        CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        CardDefaults.outlinedCardColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name.ifBlank { stringResource(R.string.place_name_placeholder) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = placeSummary(place),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = place.enabled, onCheckedChange = null, enabled = false)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.action_delete_place))
            }
        }
    }
}

@Composable
private fun PlaceEditorCard(
    place: PlaceDraft,
    onUpdatePlace: (String, (PlaceDraft) -> PlaceDraft) -> Unit,
    onAddSsid: (String) -> Unit,
    onDeleteSsid: (String, String) -> Unit,
    onUpdateSsid: (String, String, (SsidDraft) -> SsidDraft) -> Unit,
    onUseCurrentLocation: (String?) -> Unit,
) {
    InfoCard(title = stringResource(R.string.place_editor_title)) {
        OutlinedTextField(
            value = place.name,
            onValueChange = { value ->
                onUpdatePlace(place.placeId) { current -> current.copy(name = value) }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_place_name)) },
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.field_place_enabled),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            Switch(
                checked = place.enabled,
                onCheckedChange = { enabled ->
                    onUpdatePlace(place.placeId) { current -> current.copy(enabled = enabled) }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = place.latitudeText,
                onValueChange = { value ->
                    onUpdatePlace(place.placeId) { current -> current.copy(latitudeText = value) }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.field_latitude)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = place.longitudeText,
                onValueChange = { value ->
                    onUpdatePlace(place.placeId) { current -> current.copy(longitudeText = value) }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.field_longitude)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = place.radiusText,
                onValueChange = { value ->
                    onUpdatePlace(place.placeId) { current -> current.copy(radiusText = value) }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.field_radius)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedButton(onClick = { onUseCurrentLocation(place.placeId) }) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_use_current))
            }
        }
        Spacer(Modifier.height(16.dp))
        PlaceMapPreview(
            latitude = place.latitudeText.toDoubleOrNull(),
            longitude = place.longitudeText.toDoubleOrNull(),
            radiusMeters = place.radiusText.toFloatOrNull(),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.place_behavior_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.field_trigger_on_switch))
                Text(
                    text = stringResource(R.string.field_trigger_on_switch_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = place.triggerOnPlaceSwitch,
                onCheckedChange = { enabled ->
                    onUpdatePlace(place.placeId) { current ->
                        current.copy(triggerOnPlaceSwitch = enabled)
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.field_return_after_away))
                Text(
                    text = stringResource(R.string.field_return_after_away_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = place.returnAfterAwayEnabled,
                onCheckedChange = { enabled ->
                    onUpdatePlace(place.placeId) { current ->
                        current.copy(returnAfterAwayEnabled = enabled)
                    }
                },
            )
        }
        if (place.returnAfterAwayEnabled) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = place.returnAfterAwayMinutesText,
                onValueChange = { value ->
                    onUpdatePlace(place.placeId) { current ->
                        current.copy(returnAfterAwayMinutesText = value)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_return_after_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.place_ssids_title),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { onAddSsid(place.placeId) }) {
                Text(stringResource(R.string.action_add_ssid))
            }
        }
        if (place.ssids.isEmpty()) {
            Text(
                text = stringResource(R.string.place_ssids_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                place.ssids.forEach { ssid ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.place_ssid_entry_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = ssid.enabled,
                                        onCheckedChange = { enabled ->
                                            onUpdateSsid(place.placeId, ssid.ssidId) { current ->
                                                current.copy(enabled = enabled)
                                            }
                                        },
                                    )
                                    IconButton(onClick = { onDeleteSsid(place.placeId, ssid.ssidId) }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(R.string.action_delete_ssid),
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = ssid.name,
                                onValueChange = { value ->
                                    onUpdateSsid(place.placeId, ssid.ssidId) { current ->
                                        current.copy(name = value)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.field_ssid_name)) },
                            )
                            OutlinedTextField(
                                value = ssid.ssid,
                                onValueChange = { value ->
                                    onUpdateSsid(place.placeId, ssid.ssidId) { current ->
                                        current.copy(ssid = value)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.field_ssid_value)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppLanguage.entries.forEach { language ->
            FilterChip(
                selected = selected == language,
                onClick = { onSelected(language) },
                label = {
                    Text(
                        when (language) {
                            AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                            AppLanguage.FINNISH -> stringResource(R.string.language_finnish)
                        },
                    )
                },
                leadingIcon = if (selected == language) {
                    { Icon(Icons.Default.Language, contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ScreenColumn(
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content()
            },
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PrimaryActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onPrimary) {
            Text(primaryLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(onClick = onSecondary) {
                Text(secondaryLabel)
            }
        }
        if (tertiaryLabel != null && onTertiary != null) {
            TextButton(onClick = onTertiary) {
                Text(tertiaryLabel)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: @Composable () -> Unit,
    title: String,
    supporting: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        if (granted) stringResource(R.string.status_granted) else stringResource(R.string.status_missing),
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            if (!granted) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ArmStateBadge(armed: Boolean) {
    val containerColor = if (armed) Color(0xFFDBF5E0) else Color(0xFFF8E3BF)
    val contentColor = if (armed) Color(0xFF166534) else Color(0xFF9A5B00)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (armed) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = contentColor)
            } else {
                Text(
                    text = "Zzz",
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = if (armed) {
                    stringResource(R.string.summary_armed)
                } else {
                    stringResource(R.string.home_unarmed_title)
                },
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SummaryChip(label: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
    )
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun localizedReminderStatus(status: ReminderStatus): String {
    return when (status) {
        ReminderStatus.IDLE -> stringResource(R.string.summary_waiting)
        ReminderStatus.ACTIVE -> stringResource(R.string.today_status_active)
        ReminderStatus.SNOOZED -> stringResource(R.string.today_status_snoozed)
        ReminderStatus.COMPLETED -> stringResource(R.string.today_status_completed)
    }
}

@Composable
private fun localizedTriggerSource(source: TriggerSource?): String {
    return when (source) {
        TriggerSource.GEOFENCE -> stringResource(R.string.trigger_geofence)
        TriggerSource.WIFI -> stringResource(R.string.trigger_wifi)
        null -> stringResource(R.string.value_none)
    }
}

@Composable
private fun formatInstant(value: Instant?): String {
    return value?.let(instantFormatter::format) ?: stringResource(R.string.value_none)
}

@Composable
private fun placeSummary(place: PlaceDraft): String {
    val signals = buildList {
        if (place.latitudeText.isNotBlank() && place.longitudeText.isNotBlank()) {
            add(stringResource(R.string.place_signal_geofence))
        }
        val ssidCount = place.ssids.count { it.ssid.isNotBlank() }
        if (ssidCount > 0) {
            add(stringResource(R.string.place_signal_wifi_count, ssidCount))
        }
    }
    val summary = if (signals.isEmpty()) {
        stringResource(R.string.place_signal_none)
    } else {
        signals.joinToString(" • ")
    }
    return if (place.enabled) {
        summary
    } else {
        stringResource(R.string.place_disabled_summary, summary)
    }
}

private fun initialOnboardingStep(state: OnboardingState): OnboardingStep {
    return when {
        !state.privacyAcknowledged -> OnboardingStep.WHAT
        !state.completed -> OnboardingStep.PERMISSIONS
        else -> OnboardingStep.FIRST_PLACE
    }
}
