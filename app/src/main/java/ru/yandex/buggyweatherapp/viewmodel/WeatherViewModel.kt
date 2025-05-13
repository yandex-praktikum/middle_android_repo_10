package ru.yandex.buggyweatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherUiModel
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import ru.yandex.buggyweatherapp.ui.state.UiState
import ru.yandex.buggyweatherapp.utils.NetworkConnectivityManager
import ru.yandex.buggyweatherapp.utils.NetworkState
import javax.inject.Inject

/**
 * ViewModel для экрана погоды.
 * Обновлено: Удалены зависимости от контекста и форматирования. ViewModel 
 * теперь отвечает только за бизнес-логику, а представление данных - ответственность UI слоя.
 */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: IWeatherRepository,
    private val locationRepository: ILocationRepository,
    private val networkManager: NetworkConnectivityManager
) : ViewModel() {
    
    // UI состояние для погоды, используя WeatherUiModel вместо WeatherData
    private val _weatherState = MutableStateFlow<UiState<WeatherUiModel>>(UiState.Loading)
    val weatherState: StateFlow<UiState<WeatherUiModel>> = _weatherState
    
    // Текущее местоположение - используем StateFlow вместо LiveData
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation
    
    // Название города - используем StateFlow вместо LiveData
    private val _cityName = MutableStateFlow<String>("")
    val cityName: StateFlow<String> = _cityName
    
    // Состояние сети - StateFlow для основного состояния
    val isNetworkAvailable: StateFlow<Boolean> = networkManager.isNetworkAvailable
    
    // Детальные события сети
    val networkEvents: Flow<NetworkState> = networkManager.networkEvents
    
    // Задание для автообновления
    private var autoRefreshJob: Job? = null

    // Сетевые задания
    private var networkMonitorJob: Job? = null

    init {
        // Стартуем мониторинг сети и реакцию на изменения
        startNetworkMonitoring()
    }
    
    // Получение погоды для текущего местоположения
    fun fetchCurrentLocationWeather() {
        viewModelScope.launch {
            _weatherState.value = UiState.Loading

            try {
                val locationResult = locationRepository.getCurrentLocation()
                
                locationResult.fold(
                    onSuccess = { location ->
                        _currentLocation.value = location

                        // Получение названия города асинхронно
                        val cityResult = locationRepository.getCityNameFromLocation(location)
                        cityResult.fold(
                            onSuccess = { city ->
                                _cityName.value = city
                            },
                            onFailure = {
                                // Если не удалось получить название города, используем пустую строку
                                if (_cityName.value.isBlank()) {
                                    _cityName.value = "Неизвестное место"
                                }
                            }
                        )

                        // Получение данных о погоде
                        getWeatherForLocation(location)
                    },
                    onFailure = { e ->
                        _weatherState.value = UiState.Error("Не удалось получить текущее местоположение: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _weatherState.value = UiState.Error("Ошибка при получении местоположения: ${e.message}")
            }
        }
    }
    
    // Получение погоды для заданного местоположения
    fun getWeatherForLocation(location: Location) {
        viewModelScope.launch {
            _weatherState.value = UiState.Loading

            if (!networkManager.isNetworkAvailable()) {
                _weatherState.value = UiState.Error("Отсутствует подключение к сети")
                return@launch
            }

            try {
                val result = weatherRepository.getWeatherData(location)
                
                result.fold(
                    onSuccess = { data ->
                        // Создаем UI модель из данных
                        val uiModel = data.toUiModel()
                        _weatherState.value = UiState.Success(uiModel)
                    },
                    onFailure = { e ->
                        _weatherState.value = UiState.Error("Ошибка при загрузке погоды: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _weatherState.value = UiState.Error("Неизвестная ошибка: ${e.message}")
            }
        }
    }
    
    // Поиск погоды по названию города
    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            _weatherState.value = UiState.Error("Название города не может быть пустым")
            return
        }
        
        viewModelScope.launch {
            _weatherState.value = UiState.Loading

            if (!networkManager.isNetworkAvailable()) {
                _weatherState.value = UiState.Error("Отсутствует подключение к сети")
                return@launch
            }
            
            try {
                val result = weatherRepository.getWeatherByCity(city)

                result.fold(
                    onSuccess = { data ->
                        // Создаем UI модель из данных
                        val uiModel = data.toUiModel()
                        _weatherState.value = UiState.Success(uiModel)
                        _cityName.value = data.cityName
                        _currentLocation.value = Location(
                            latitude = data.latitude ?: 0.0,
                            longitude = data.longitude ?: 0.0,
                            name = data.cityName
                        )
                    },
                    onFailure = { e ->
                        _weatherState.value = UiState.Error("Ошибка при загрузке погоды: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _weatherState.value = UiState.Error("Неизвестная ошибка: ${e.message}")
            }
        }
    }
    
    // Вместо форматирования в ViewModel, данные передаются в UI слой,
    // где они будут отформатированы с использованием строковых ресурсов.
    // Это соответствует принципу разделения ответственности.

    // Запуск автоматического обновления данных
    fun startAutoRefresh() {
        // Отменяем предыдущее задание, если оно существует
        autoRefreshJob?.cancel()

        // Запускаем новое задание
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60000) // Обновление каждую минуту
                _currentLocation.value?.let { location ->
                    getWeatherForLocation(location)
                }
            }
        }
    }

    // Остановка автоматического обновления
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * Запускает мониторинг состояния сети и реакцию на изменения
     */
    private fun startNetworkMonitoring() {
        networkMonitorJob?.cancel()
        networkMonitorJob = viewModelScope.launch {
            // Мониторим изменения и реагируем на них
            networkEvents.collectLatest { state ->
                when (state) {
                    is NetworkState.Available -> {
                        if (_weatherState.value is UiState.Error) {
                            // Если у нас была ошибка из-за сети, пробуем загрузить заново
                            _currentLocation.value?.let { location ->
                                getWeatherForLocation(location)
                            } ?: fetchCurrentLocationWeather()
                        }
                    }
                    is NetworkState.Lost,
                    is NetworkState.Unavailable -> {
                        // При потере сети сообщаем пользователю, но только если активно загружаем данные
                        if (_weatherState.value is UiState.Loading) {
                            _weatherState.value = UiState.Error("Отсутствует подключение к сети")
                        }
                    }
                    is NetworkState.CapabilitiesChanged -> {
                        // Если сеть стала доступна, но была ошибка, пробуем заново
                        if (state.hasInternet && state.hasValidated && _weatherState.value is UiState.Error) {
                            _currentLocation.value?.let { location ->
                                getWeatherForLocation(location)
                            }
                        }
                    }
                    else -> { /* Другие состояния не обрабатываем */ }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Отменяем автоматическое обновление
        stopAutoRefresh()

        // Останавливаем мониторинг сети
        networkMonitorJob?.cancel()
        networkMonitorJob = null

        // Освобождаем ресурсы
        viewModelScope.cancel()
    }
}