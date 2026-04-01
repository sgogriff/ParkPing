package com.gowain.parkping.ui

import com.gowain.parkping.model.DailyReminderState
import com.gowain.parkping.model.MonitoringStatus
import com.gowain.parkping.model.OnboardingState
import com.gowain.parkping.model.ParkPingSettings
import com.gowain.parkping.model.PermissionSnapshot
import java.time.LocalDate

data class AppUiState(
    val settings: ParkPingSettings = ParkPingSettings(),
    val dailyReminderState: DailyReminderState = DailyReminderState(),
    val monitoringStatus: MonitoringStatus = MonitoringStatus(),
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val setupDraft: SetupDraft = SetupDraft(),
    val onboardingState: OnboardingState = OnboardingState(),
    val currentBusinessDate: LocalDate = LocalDate.now(),
    val statusMessage: String? = null,
)
