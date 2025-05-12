package ru.yandex.buggyweatherapp.repository

import android.content.Context
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.utils.LocationTracker
import java.util.Locale

// ОШИБКА: Отсутствует интерфейс для инверсии зависимостей
class LocationRepository(
    // ОШИБКА: Прямое использование контекста без учета жизненного цикла
    private val context: Context
) {
    // ОШИБКА: Прямая инициализация тяжелых объектов без правильной очистки
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    // ОШИБКА: Отсутствует потокобезопасность
    private var currentLocation: Location? = null
    
    // ОШИБКА: Утечка памяти - сохранение ссылки на колбэк
    private var locationCallback: ((Location?) -> Unit)? = null
    
    // ОШИБКА: Создание новых экземпляров колбэка для каждого запроса
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        try {
            locationCallback = callback
            
            // ОШИБКА: Отсутствует проверка разрешений перед запросом местоположения
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val userLocation = Location(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        currentLocation = userLocation
                        callback(userLocation)
                    } else {
                        // ОШИБКА: Запуск обновлений местоположения без правильной очистки
                        requestLocationUpdates(callback)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LocationRepository", "Error getting location", e)
                    callback(null)
                }
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Location permission not granted", e)
            callback(null)
        }
    }
    
    // ОШИБКА: Отсутствует правильная остановка обновлений местоположения
    private fun requestLocationUpdates(callback: (Location?) -> Unit) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build()
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val userLocation = Location(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        currentLocation = userLocation
                        callback(userLocation)
                        
                        // ОШИБКА: Не удаляются обновления после получения местоположения
                    }
                }
            }
            
            // ОШИБКА: Использование Looper.getMainLooper() для обновлений местоположения
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Location permission not granted", e)
            callback(null)
        }
    }
    
    // ОШИБКА: Тяжелая операция в UI-потоке
    fun getCityNameFromLocation(location: Location): String? {
        try {
            // ОШИБКА: Создание нового экземпляра Geocoder для каждого вызова
            val geocoder = Geocoder(context, Locale.getDefault())
            
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            return if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                if (address.locality != null) {
                    address.locality
                } else if (address.subAdminArea != null) {
                    address.subAdminArea
                } else {
                    address.adminArea
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Error getting city name", e)
            return null
        }
    }
    
    // ОШИБКА: Делегирование другому синглтону вместо внедрения зависимостей
    fun startLocationTracking() {
        LocationTracker.getInstance(context).startTracking()
    }
    
    // ОШИБКА: Отсутствует метод очистки для освобождения ресурсов
}