package ru.yandex.buggyweatherapp.utils

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

class LocationTracker private constructor(context: Context) {
    // Использование контекста приложения вместо активити
    private val appContext = context.applicationContext

    // Создаем обработчик исключений для корутин
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Unhandled exception in location tracker coroutine: ${exception.message}", exception)
    }

    // Область видимости корутин для трекера с обработчиком исключений
    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    companion object {
        private const val TAG = "LocationTracker"

        @Volatile
        private var instance: LocationTracker? = null
        
        fun getInstance(context: Context): LocationTracker {
            return instance ?: synchronized(this) {
                instance ?: LocationTracker(context.applicationContext).also { instance = it }
            }
        }

        // Метод для очистки экземпляра (для тестирования)
        fun clearInstance() {
            synchronized(this) {
                instance?.stopTracking()
                instance = null
            }
        }
    }
    
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listeners = CopyOnWriteArrayList<(ru.yandex.buggyweatherapp.model.Location) -> Unit>()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val newLocation = ru.yandex.buggyweatherapp.model.Location(
                latitude = location.latitude,
                longitude = location.longitude
            )
            
            // Используем собственную область видимости корутин с обработкой исключений
            trackerScope.launch {
                try {
                    notifyListeners(newLocation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying listeners: ${e.message}", e)
                }
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        
        override fun onProviderEnabled(provider: String) {}
        
        override fun onProviderDisabled(provider: String) {
            Log.d("LocationTracker", "Provider disabled: $provider")
        }
    }
    
    /**
     * Начинает отслеживание местоположения с использованием наиболее подходящего провайдера
     * Обрабатывает все возможные исключения и логирует их
     * @return true если отслеживание успешно запущено, false в противном случае
     */
    fun startTracking(): Boolean {
        var trackingStarted = false

        try {
            // Проверка доступности провайдеров
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            when {
                isGpsEnabled -> {
                    try {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            5000, // 5 секунд
                            10f, // 10 метров
                            locationListener
                        )
                        Log.d(TAG, "Started tracking with GPS provider")
                        trackingStarted = true
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission denied for GPS provider", e)
                        // Пробуем другой провайдер при ошибке
                        if (isNetworkEnabled) {
                            tryStartNetworkProvider()?.let { trackingStarted = it }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting GPS tracking", e)
                        if (isNetworkEnabled) {
                            tryStartNetworkProvider()?.let { trackingStarted = it }
                        }
                    }
                }
                isNetworkEnabled -> {
                    tryStartNetworkProvider()?.let { trackingStarted = it }
                }
                else -> {
                    Log.e(TAG, "No location provider available")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
        }

        return trackingStarted
    }

    /**
     * Вспомогательный метод для запуска сетевого провайдера
     * @return true если успешно, false при ошибке, null если метод не был вызван
     */
    private fun tryStartNetworkProvider(): Boolean? {
        return try {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000,
                10f,
                locationListener
            )
            Log.d(TAG, "Started tracking with Network provider")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for Network provider", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Network tracking", e)
            false
        }
    }
    
    /**
     * Останавливает отслеживание местоположения и отменяет все корутины
     */
    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
            Log.d(TAG, "Location tracking stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when stopping tracking", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        } finally {
            // Отменяем все корутины при остановке отслеживания
            trackerScope.cancel("Location tracking stopped")
        }
    }

    /**
     * Добавляет слушателя для обновлений местоположения
     * @param listener функция обратного вызова
     */
    fun addListener(listener: (ru.yandex.buggyweatherapp.model.Location) -> Unit) {
        listeners.add(listener)
        Log.d(TAG, "Listener added, total listeners: ${listeners.size}")
    }

    /**
     * Удаляет слушателя
     * @param listener функция обратного вызова для удаления
     */
    fun removeListener(listener: (ru.yandex.buggyweatherapp.model.Location) -> Unit) {
        listeners.remove(listener)
        Log.d(TAG, "Listener removed, total listeners: ${listeners.size}")
    }

    /**
     * Очищает все слушатели
     */
    fun clearListeners() {
        val previousCount = listeners.size
        listeners.clear()
        Log.d(TAG, "All listeners cleared, removed count: $previousCount")
    }
    
    /**
     * Уведомляет всех слушателей о новом местоположении
     * Обрабатывает исключения для каждого слушателя отдельно
     * @param location новое местоположение
     */
    private suspend fun notifyListeners(location: ru.yandex.buggyweatherapp.model.Location) {
        // Уведомление в главном потоке
        withContext(Dispatchers.Main) {
            if (listeners.isEmpty()) {
                Log.d(TAG, "No listeners to notify about location update")
                return@withContext
            }

            Log.d(TAG, "Notifying ${listeners.size} listeners about location update")
            for (listener in listeners) {
                try {
                    listener(location)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying listener: ${e.message}", e)
                    // Продолжаем уведомлять остальных слушателей даже при ошибке
                }
            }
        }
    }
}