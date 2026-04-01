package com.gowain.parkping.model

data class OnboardingState(
    val privacyAcknowledged: Boolean = false,
    val completed: Boolean = false,
)
