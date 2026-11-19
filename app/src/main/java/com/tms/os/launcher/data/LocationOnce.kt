package com.tms.os.launcher.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * The phone's position, asked for once and only when the user taps "use my
 * location" — never in the background, never on a timer. The approximate
 * (network) position is enough for the weather; GPS is not switched on.
 */
object LocationOnce {

    class Place(val latitude: Double, val longitude: Double, val name: String?)

    /** Null when no position could be had (location off, no provider, timeout). Needs the location permission. */
    @SuppressLint("MissingPermission")
    suspend fun find(context: Context): Place? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        if (providers.isEmpty()) return null

        // A recent fix some app already obtained costs nothing at all.
        val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 2 * 60 * 60 * 1000L }
            .maxByOrNull { it.time }
        val location = recent ?: fresh(manager, providers) ?: return null
        return Place(location.latitude, location.longitude, name(context, location))
    }

    @SuppressLint("MissingPermission")
    private suspend fun fresh(manager: LocationManager, providers: List<String>): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val provider = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER).firstOrNull { it in providers }
            ?: providers.first()
        return withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine { continuation ->
                val signal = android.os.CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                runCatching {
                    manager.getCurrentLocation(provider, signal, Runnable::run) { found -> if (continuation.isActive) continuation.resume(found) }
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            }
        }
    }

    /** The town for a position, if this phone can look that up; otherwise the caller shows a generic label. */
    private fun name(context: Context, location: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        @Suppress("DEPRECATION")
        val address = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        address?.locality ?: address?.subAdminArea ?: address?.adminArea
    }.getOrNull()
}
