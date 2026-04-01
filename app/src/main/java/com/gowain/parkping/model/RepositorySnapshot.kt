package com.gowain.parkping.model

data class RepositorySnapshot(
    val settings: ParkPingSettings = ParkPingSettings(),
    val dailyReminderState: DailyReminderState = DailyReminderState(),
    val monitoringStatus: MonitoringStatus = MonitoringStatus(),
    val onboardingState: OnboardingState = OnboardingState(),
)
