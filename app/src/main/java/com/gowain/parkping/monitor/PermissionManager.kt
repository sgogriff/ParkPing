package com.gowain.parkping.monitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.gowain.parkping.model.PermissionSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun snapshot(): PermissionSnapshot {
        val fineLocationGranted = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            fineLocationGranted
        }
        return PermissionSnapshot(
            notificationsGranted = notificationsGranted,
            fineLocationGranted = fineLocationGranted,
            backgroundLocationGranted = backgroundLocationGranted,
        )
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
