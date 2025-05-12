package ru.yandex.buggyweatherapp.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.WeatherApplication
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import ru.yandex.buggyweatherapp.repository.LocationRepository
import ru.yandex.buggyweatherapp.repository.WeatherRepository
import ru.yandex.buggyweatherapp.utils.ImageLoader
import java.util.Timer
import java.util.TimerTask

// ОШИБКА: ViewModel со слишком большим количеством обязанностей (Божественная ViewModel)
class WeatherViewModel : ViewModel() {
    
    // ОШИБКА: Прямая инициализация без внедрения зависимостей
    // ОШИБКА: Хранение контекста активити
    private lateinit var activityContext: Context
    
    // ОШИБКА: Прямое создание экземпляра репозитория вместо DI
    private val weatherRepository = WeatherRepository()
    private val locationRepository by lazy { 
        LocationRepository(activityContext)  // ОШИБКА: Использование контекста активити в репозитории
    }
    
    // ОШИБКА: Слишком много объектов LiveData
    val weatherData = MutableLiveData<WeatherData>()
    val currentLocation = MutableLiveData<Location>()
    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()
    val cityName = MutableLiveData<String>()
    
    // ОШИБКА: Область корутин не привязана к жизненному циклу ViewModel
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    // ОШИБКА: Таймер не отменяется в onCleared
    private var refreshTimer: Timer? = null
    
    // ОШИБКА: Хранение контекста активити
    fun initialize(context: Context) {
        this.activityContext = context
        fetchCurrentLocationWeather()
        
        // ОШИБКА: Запуск таймера без учета жизненного цикла
        startAutoRefresh()
    }
    
    // ОШИБКА: Отсутствует обработка ошибок
    fun fetchCurrentLocationWeather() {
        isLoading.value = true
        error.value = null
        
        locationRepository.getCurrentLocation { location ->
            if (location != null) {
                currentLocation.value = location
                
                // ОШИБКА: Синхронная операция, блокирующая UI
                val cityNameFromLocation = locationRepository.getCityNameFromLocation(location)
                cityName.value = cityNameFromLocation
                
                getWeatherForLocation(location)
            } else {
                isLoading.value = false
                error.value = "Unable to get current location"
            }
        }
    }
    
    fun getWeatherForLocation(location: Location) {
        isLoading.value = true
        error.value = null
        
        weatherRepository.getWeatherData(location) { data, exception ->
            // ОШИБКА: Прямое обновление UI-потока без использования диспетчера
            Handler(Looper.getMainLooper()).post {
                isLoading.value = false
                
                if (data != null) {
                    weatherData.value = data
                } else {
                    error.value = exception?.message ?: "Unknown error"
                }
            }
        }
    }
    
    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            error.value = "City name cannot be empty"
            return
        }
        
        isLoading.value = true
        error.value = null
        
        // ОШИБКА: Не используются корутины для сетевых операций
        weatherRepository.getWeatherByCity(city) { data, exception ->
            // ОШИБКА: Прямое обновление LiveData из колбэка без переключения на главный поток
            isLoading.value = false
            
            if (data != null) {
                weatherData.value = data
                cityName.value = data.cityName
                currentLocation.value = Location(0.0, 0.0, data.cityName)
            } else {
                error.value = exception?.message ?: "Unknown error"
            }
        }
    }
    
    // ОШИБКА: Бизнес-логика в ViewModel
    fun formatTemperature(temp: Double): String {
        return "${temp.toInt()}°C"
    }
    
    // ОШИБКА: Тяжелая загрузка изображений в ViewModel
    fun loadWeatherIcon(iconCode: String) {
        coroutineScope.launch {
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            ImageLoader.loadImage(iconUrl)
        }
    }
    
    // ОШИБКА: Таймер не учитывает жизненный цикл
    private fun startAutoRefresh() {
        refreshTimer = Timer()
        refreshTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                currentLocation.value?.let { location ->
                    getWeatherForLocation(location)
                }
            }
        }, 60000, 60000) // Обновление каждую минуту
    }
    
    // ОШИБКА: Не обрабатываются ошибки бизнес-логики
    fun toggleFavorite() {
        weatherData.value?.let {
            it.isFavorite = !it.isFavorite
            // ОШИБКА: Прямое изменение объекта LiveData
            weatherData.value = it
        }
    }
    
    // ОШИБКА: Очищаются только некоторые ресурсы
    override fun onCleared() {
        super.onCleared()
        // ОШИБКА: Не отменяются корутины
        // ОШИБКА: Не отменяется таймер
    }
}