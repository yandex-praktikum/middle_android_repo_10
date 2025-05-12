package ru.yandex.buggyweatherapp.utils

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

// ОШИБКА: Неправильно реализованный паттерн Singleton
class LocationTracker private constructor(
    // ОШИБКА: Хранение контекста активити вместо контекста приложения
    private val context: Context
) {
    // ОШИБКА: Статический экземпляр, который может вызывать утечки памяти
    companion object {
        @Volatile
        private var instance: LocationTracker? = null
        
        fun getInstance(context: Context): LocationTracker {
            return instance ?: synchronized(this) {
                instance ?: LocationTracker(context).also { instance = it }
            }
        }
    }
    
    // ОШИБКА: Прямое использование системного сервиса без проверки разрешений
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // ОШИБКА: Список слушателей без механизма очистки
    private val listeners = CopyOnWriteArrayList<(ru.yandex.buggyweatherapp.model.Location) -> Unit>()
    
    // ОШИБКА: Запуск слушателя в конструкторе
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // ОШИБКА: Создание нового объекта локации для каждого обновления
            val newLocation = ru.yandex.buggyweatherapp.model.Location(
                latitude = location.latitude,
                longitude = location.longitude
            )
            
            // ОШИБКА: Уведомление в том же потоке, в котором вызывается onLocationChanged
            notifyListeners(newLocation)
        }
        
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        
        override fun onProviderEnabled(provider: String) {}
        
        override fun onProviderDisabled(provider: String) {}
    }
    
    // ОШИБКА: Отсутствуют проверки разрешений
    fun startTracking() {
        try {
            // ОШИБКА: Не проверяется доступность провайдера
            // ОШИБКА: Запросы местоположения в основном потоке
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // 5 секунд
                10f, // 10 метров
                locationListener
            )
            
            // ОШИБКА: Не обрабатывается случай, когда GPS недоступен
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "Permission denied", e)
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error starting location tracking", e)
        }
    }
    
    // ОШИБКА: addListener без соответствующего removeListener
    fun addListener(listener: (ru.yandex.buggyweatherapp.model.Location) -> Unit) {
        listeners.add(listener)
    }
    
    private fun notifyListeners(location: ru.yandex.buggyweatherapp.model.Location) {
        // ОШИБКА: UI-поток используется для всех колбэков независимо от потребностей слушателя
        Handler(Looper.getMainLooper()).post {
            for (listener in listeners) {
                listener(location)
            }
        }
    }
    
    // ОШИБКА: Отсутствует метод stopTracking() для удаления обновлений местоположения
    // ОШИБКА: Отсутствует метод очистки для слушателей
}