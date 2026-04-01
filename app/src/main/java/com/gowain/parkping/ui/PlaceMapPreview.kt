package com.gowain.parkping.ui

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gowain.parkping.R
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun PlaceMapPreview(
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Float?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasCoordinates = latitude != null && longitude != null
    val isOnline = remember(context) { context.isOnline() }
    val shape = RoundedCornerShape(24.dp)

    if (!hasCoordinates) {
        MapPlaceholder(
            modifier = modifier,
            title = stringResource(R.string.map_preview_waiting_title),
            body = stringResource(R.string.map_preview_waiting_body),
        )
        return
    }

    if (!isOnline) {
        MapPlaceholder(
            modifier = modifier,
            title = stringResource(R.string.map_preview_offline_title),
            body = stringResource(
                R.string.map_preview_coordinates_body,
                latitude,
                longitude,
            ),
        )
        return
    }

    val point = remember(latitude, longitude) { GeoPoint(latitude, longitude) }
    val resolvedRadius = (radiusMeters ?: 200f).coerceAtLeast(50f)
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            isClickable = false
            isFocusable = false
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        factory = { mapView },
        update = { mapView ->
            mapView.controller.setZoom(zoomForRadius(resolvedRadius))
            mapView.controller.setCenter(point)
            mapView.overlays.clear()

            val radiusOverlay = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(point, resolvedRadius.toDouble())
                fillColor = Color.argb(40, 26, 83, 108)
                strokeColor = Color.argb(230, 26, 83, 108)
                strokeWidth = 4f
            }
            val marker = Marker(mapView).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = null
            }
            mapView.overlays.add(radiusOverlay)
            mapView.overlays.add(marker)
            mapView.invalidate()
        },
    )
}

@Composable
private fun MapPlaceholder(
    modifier: Modifier,
    title: String,
    body: String,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun zoomForRadius(radiusMeters: Float): Double {
    return when {
        radiusMeters <= 75f -> 17.5
        radiusMeters <= 150f -> 17.0
        radiusMeters <= 300f -> 16.4
        radiusMeters <= 600f -> 15.6
        radiusMeters <= 1_000f -> 14.8
        else -> 14.0
    }
}

private fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService<ConnectivityManager>() ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
