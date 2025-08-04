package com.example.allote.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class Coordinates(val latitude: Double, val longitude: Double)
data class LocationDetails(val coordinates: Coordinates, val cityName: String?)

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocationDetails(): LocationDetails? {
        val coordinates = getLastKnownCoordinates() ?: return null

        return withContext(Dispatchers.IO) {
            val cityName = getCityNameFromCoordinates(coordinates)
            LocationDetails(coordinates, cityName)
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownCoordinates(): Coordinates? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w("LocationProvider", "Location permission not granted.")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(Coordinates(location.latitude, location.longitude))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    Log.e("LocationProvider", "Failed to get location.", it)
                    continuation.resume(null)
                }
                .addOnCanceledListener { continuation.cancel() }
        }
    }

    private suspend fun getCityNameFromCoordinates(coordinates: Coordinates): String {
        if (!Geocoder.isPresent()) {
            Log.w("LocationProvider", "Geocoder not present on this device.")
            return "Ubicación no disponible"
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        val defaultName = "Ubicación desconocida"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val locationName = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: defaultName
                        Log.d("LocationProvider", "Tiramisu Geocoder found: locality='${address?.locality}', subAdminArea='${address?.subAdminArea}', adminArea='${address?.adminArea}'")
                        continuation.resume(locationName)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)
                }
                val address = addresses?.firstOrNull()
                val locationName = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: defaultName
                Log.d("LocationProvider", "Legacy Geocoder found: locality='${address?.locality}', subAdminArea='${address?.subAdminArea}', adminArea='${address?.adminArea}'")
                locationName
            }
        } catch (e: Exception) {
            Log.e("LocationProvider", "Error getting city name from Geocoder", e)
            defaultName
        }
    }
}
