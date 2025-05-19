package ru.yandex.buggyweatherapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay  // Импорт для использования задержки в корутинах
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive  // Импорт для проверки активности корутины
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.R
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import ru.yandex.buggyweatherapp.utils.UiState
import ru.yandex.buggyweatherapp.utils.WeatherIconMapper
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: IWeatherRepository,
    private val locationRepository: ILocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _weatherUiState = MutableStateFlow<UiState<WeatherData>>(UiState.Loading)
    val weatherUiState: StateFlow<UiState<WeatherData>> = _weatherUiState.asStateFlow()
    
    private val _locationState = MutableStateFlow<Location?>(null)
    val locationState: StateFlow<Location?> = _locationState.asStateFlow()
    
    private val _cityNameState = MutableStateFlow<String?>(null)
    val cityNameState: StateFlow<String?> = _cityNameState.asStateFlow()
    
    // Заменили Timer на Job для безопасного периодического обновления
    private var refreshJob: Job? = null
    private var fetchWeatherJob: Job? = null
    
    init {
        fetchCurrentLocationWeather()
        startAutoRefresh()
    }
    
    fun fetchCurrentLocationWeather() {
        _weatherUiState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                locationRepository.getCurrentLocation().fold(
                    onSuccess = { location ->
                        _locationState.value = location
                        
                        // Получаем название города
                        locationRepository.getCityNameFromLocation(location).fold(
                            onSuccess = { cityName ->
                                _cityNameState.value = cityName
                            },
                            onFailure = { error ->
                                Log.e("WeatherViewModel", "Error getting city name", error)
                            }
                        )
                        
                        // Получаем данные о погоде
                        getWeatherForLocation(location)
                    },
                    onFailure = { error ->
                        _weatherUiState.value = UiState.Error(
                            context.getString(R.string.location_error) + ": ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _weatherUiState.value = UiState.Error(
                    context.getString(R.string.location_error) + ": ${e.message}"
                )
            }
        }
    }
    
    fun getWeatherForLocation(location: Location) {
        fetchWeatherJob?.cancel()
        
        fetchWeatherJob = viewModelScope.launch {
            _weatherUiState.value = UiState.Loading
            
            try {
                weatherRepository.getWeatherData(location).fold(
                    onSuccess = { data ->
                        _weatherUiState.value = UiState.Success(data)
                    },
                    onFailure = { error ->
                        _weatherUiState.value = UiState.Error(
                            error.message ?: context.getString(R.string.network_error)
                        )
                    }
                )
            } catch (e: Exception) {
                _weatherUiState.value = UiState.Error(
                    context.getString(R.string.network_error) + ": ${e.message}"
                )
            }
        }
    }
    
    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            _weatherUiState.value = UiState.Error(context.getString(R.string.empty_city_error))
            return
        }
        
        fetchWeatherJob?.cancel()
        fetchWeatherJob = viewModelScope.launch {
            _weatherUiState.value = UiState.Loading
            
            try {
                weatherRepository.getWeatherByCity(city).fold(
                    onSuccess = { data ->
                        _weatherUiState.value = UiState.Success(data)
                        _cityNameState.value = data.cityName
                        _locationState.value = Location(0.0, 0.0, data.cityName)
                    },
                    onFailure = { error ->
                        _weatherUiState.value = UiState.Error(
                            error.message ?: context.getString(R.string.network_error)
                        )
                    }
                )
            } catch (e: Exception) {
                _weatherUiState.value = UiState.Error(
                    context.getString(R.string.network_error) + ": ${e.message}"
                )
            }
        }
    }
    
    fun getWeatherIconUrl(iconCode: String): String {
        return WeatherIconMapper.getIconUrl(iconCode)
    }
    
    // Метод toggleFavorite был удален, так как функциональность избранного не используется
    
    /**
     * Запускает автоматическое обновление погоды с использованием корутин вместо Timer.
     * Это решает проблему с scheduleAtFixedRate, который может вызвать неожиданное поведение
     * когда процессы Android кэшируются и возобновляются.
     */
    private fun startAutoRefresh() {
        refreshJob?.cancel() // Отменяем предыдущую задачу, если она существует
        refreshJob = viewModelScope.launch {
            while (isActive) { // Проверяем, активна ли корутина
                delay(60000) // Задержка в 60 секунд перед первым обновлением
                _locationState.value?.let { location ->
                    getWeatherForLocation(location)
                }
            }
        }
    }
    
    /**
     * Очищает ресурсы при уничтожении ViewModel.
     * Отменяет все запущенные корутины и останавливает обновления локации.
     */
    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel() // Отменяем задачу автообновления
        fetchWeatherJob?.cancel() // Отменяем текущий запрос погоды
        locationRepository.stopLocationUpdates()
    }
}