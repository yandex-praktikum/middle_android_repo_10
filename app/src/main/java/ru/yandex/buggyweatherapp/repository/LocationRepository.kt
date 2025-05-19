package ru.yandex.buggyweatherapp.repository

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.model.Location
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepository @Inject constructor(
    private val application: Application
) : ILocationRepository {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(application)
    
    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Location> = 
        suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val userLocation = Location(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            currentLocation = userLocation
                            continuation.resume(Result.success(userLocation))
                        } else {
                            requestNewLocation(continuation)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationRepository", "Error getting location", e)
                        continuation.resume(Result.failure(e))
                    }
                
                continuation.invokeOnCancellation {
                    // Отмена операции если корутина была отменена
                    locationCallback?.let { callback ->
                        fusedLocationClient.removeLocationUpdates(callback)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("LocationRepository", "Location permission not granted", e)
                continuation.resume(Result.failure(e))
            }
        }
    
    @SuppressLint("MissingPermission")
    private fun requestNewLocation(continuation: kotlinx.coroutines.CancellableContinuation<Result<Location>>) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build()
            
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val userLocation = Location(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        currentLocation = userLocation
                        
                        // Прекращаем запрашивать обновления после получения первой локации
                        stopLocationUpdates()
                        
                        if (continuation.isActive) {
                            continuation.resume(Result.success(userLocation))
                        }
                    }
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Location permission not granted", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    override suspend fun getCityNameFromLocation(location: Location): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(application, Locale.getDefault())
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Для Android 13+
                    var result: Result<String?> = Result.success(null)
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val cityName = when {
                                address.locality != null -> address.locality
                                address.subAdminArea != null -> address.subAdminArea
                                else -> address.adminArea
                            }
                            result = Result.success(cityName)
                        }
                    }
                    return@withContext result
                } else {
                    // Для более старых версий Android
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    
                    return@withContext if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val cityName = when {
                            address.locality != null -> address.locality
                            address.subAdminArea != null -> address.subAdminArea
                            else -> address.adminArea
                        }
                        Result.success(cityName)
                    } else {
                        Result.success(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationRepository", "Error getting city name", e)
                return@withContext Result.failure(e)
            }
        }
    
    override fun startLocationUpdates() {
        // Здесь можно добавить логику для периодического обновления локации
    }
    
    override fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }
}