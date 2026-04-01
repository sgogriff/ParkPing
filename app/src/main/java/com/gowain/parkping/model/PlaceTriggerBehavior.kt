package com.gowain.parkping.model

data class PlaceTriggerBehavior(
    val triggerOnPlaceSwitch: Boolean = false,
    val returnAfterAwayMinutes: Int? = null,
)
