package ru.yandex.buggyweatherapp.repository

import android.content.Context
import android.location.Address
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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.utils.LocationTracker
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

// Интерфейс для инверсии зависимостей
interface ILocationRepository {
    suspend fun getCurrentLocation(): Result<Location>
    suspend fun getCityNameFromLocation(location: Location): Result<String>
    fun startLocationTracking()
    fun stopLocationTracking()
}

class LocationRepository(context: Context) : ILocationRepository {
    // Использование контекста приложения вместо активити
    private val appContext = context.applicationContext

    // Константа для тегов логирования
    private companion object {
        private const val TAG = "LocationRepository"
    }

    // Создаем собственную область видимости корутин для репозитория
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(appContext)
    
    // Потокобезопасное хранение текущего местоположения
    private val currentLocation = AtomicReference<Location?>(null)
    
    // Кэширование Geocoder
    private val geocoder by lazy { Geocoder(appContext, Locale.getDefault()) }
    
    // Сохранение ссылки на колбэк для освобождения
    private var locationCallbackReference: LocationCallback? = null

    // Преобразование в suspend функцию вместо колбэка
    override suspend fun getCurrentLocation(): Result<Location> {
        return withContext(Dispatchers.IO) {
            try {
                // Пробуем получить последнее известное местоположение через суспенд-функцию
                val location = getLastLocationSuspend()

                if (location != null) {
                    val userLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    currentLocation.set(userLocation)
                    return@withContext Result.success(userLocation)
                } else {
                    // Если последнее местоположение недоступно, запрашиваем обновления
                    try {
                        val locationResult = requestLocationUpdatesSuspend()
                        if (locationResult != null) {
                            return@withContext Result.success(locationResult)
                        } else {
                            return@withContext Result.failure(Exception("Unable to get location"))
                        }
                    } catch (e: Exception) {
                        return@withContext Result.failure(e)
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission not granted", e)
                return@withContext Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                return@withContext Result.failure(e)
            }
        }
    }

    // Получение последнего известного местоположения в виде suspend функции
    private suspend fun getLastLocationSuspend(): android.location.Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location) {}
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting location", e)
                        continuation.resume(null) {}
                    }
                    .addOnCanceledListener {
                        continuation.cancel()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getLastLocationSuspend", e)
                continuation.resume(null) {}
            }
        }
    }
    
    // Асинхронное получение местоположения через suspend функцию
    private suspend fun requestLocationUpdatesSuspend(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            val userLocation = Location(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )

                            currentLocation.set(userLocation)

                            // Удаляем обновления после получения местоположения
                            fusedLocationClient.removeLocationUpdates(this)
                            locationCallbackReference = null

                            if (continuation.isActive) {
                                continuation.resume(userLocation)
                            }
                        }
                    }
                }

                locationCallbackReference = callback

                // Используем Looper.getMainLooper() для получения обновлений
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )

                // Установка тайм-аута для получения местоположения (30 секунд)
                repositoryScope.launch {
                    delay(30000)
                    if (continuation.isActive) {
                        fusedLocationClient.removeLocationUpdates(callback)
                        locationCallbackReference = null
                        continuation.resume(null)
                    }
                }

                // Отмена при отмене корутины
                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(callback)
                    locationCallbackReference = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting location updates", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }
    
    /**
     * Асинхронное получение имени города с использованием новой API Geocoder
     * Учитывает версию Android и использует соответствующий метод
     */
    override suspend fun getCityNameFromLocation(location: Location): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Используем новую асинхронную API для Android 13+
                return@withContext suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val cityName = extractCityNameFromAddress(addresses[0])
                            if (cityName != null) {
                                continuation.resume(Result.success(cityName)) {}
                            } else {
                                continuation.resume(Result.failure(Exception("City name not found"))) {}
                            }
                        } else {
                            continuation.resume(Result.failure(Exception("Address not found"))) {}
                        }
                    }
                }
            } else {
                // Используем старый синхронный API для Android 12 и ниже
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val cityName = extractCityNameFromAddress(addresses[0])
                    if (cityName != null) {
                        return@withContext Result.success(cityName)
                    } else {
                        return@withContext Result.failure(Exception("City name not found"))
                    }
                } else {
                    return@withContext Result.failure(Exception("Address not found"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting city name: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Вспомогательный метод для извлечения названия города из объекта Address
     * Пробует различные поля адреса в порядке приоритета
     */
    private fun extractCityNameFromAddress(address: Address): String? {
        return when {
            address.locality != null -> address.locality
            address.subAdminArea != null -> address.subAdminArea
            address.adminArea != null -> address.adminArea
            // Дополнительная проверка для некоторых стран, где формат адреса отличается
            address.subLocality != null -> address.subLocality
            else -> null
        }
    }

    override fun startLocationTracking() {
        LocationTracker.getInstance(appContext).startTracking()
    }

    override fun stopLocationTracking() {
        LocationTracker.getInstance(appContext).stopTracking()

        // Освобождаем ресурсы
        locationCallbackReference?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallbackReference = null
        }
    }
    
    // Метод для очистки ресурсов
    fun cleanup() {
        stopLocationTracking()
        // Отменяем все корутины в скоупе при очистке ресурсов
        repositoryScope.cancel()
        LocationTracker.clearInstance()
    }
}