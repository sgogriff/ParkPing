package com.gowain.parkping.monitor

data class MonitoringSyncResult(
    val registered: Boolean,
    val errorMessage: String? = null,
)
