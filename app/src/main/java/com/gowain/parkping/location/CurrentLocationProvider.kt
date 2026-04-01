package com.gowain.parkping.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

@Singleton
class CurrentLocationProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): LatLng? = suspendCancellableCoroutine { continuation ->
        val tokenSource = CancellationTokenSource()
        continuation.invokeOnCancellation { tokenSource.cancel() }
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}
