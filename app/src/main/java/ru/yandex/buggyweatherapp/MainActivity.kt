package ru.yandex.buggyweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ru.yandex.buggyweatherapp.ui.screens.WeatherScreen
import ru.yandex.buggyweatherapp.ui.theme.BuggyWeatherAppTheme
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel

/**
 * Главная активность приложения.
 * 
 * Изменения:
 * 1. Добавлена аннотация @AndroidEntryPoint для поддержки Hilt
 * 2. Использование viewModels() для получения ViewModel
 * 3. Реализована корректная обработка разрешений на местоположение
 * 4. Использование ActivityResultContracts вместо устаревших onRequestPermissionsResult
 * 5. Добавлено сообщение пользователю при отказе в разрешениях
 * 6. Включен режим edge-to-edge для современного UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    /**
     * Инициализация ViewModel с помощью Hilt.
     * Ранее использовался небезопасный способ создания ViewModel вручную.
     */
    private val weatherViewModel: WeatherViewModel by viewModels()
    
    /**
     * Контракт для запроса разрешений на местоположение.
     * Обрабатывает результаты запроса и выполняет соответствующие действия.
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Точное местоположение разрешено
                weatherViewModel.fetchCurrentLocationWeather()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Примерное местоположение разрешено
                weatherViewModel.fetchCurrentLocationWeather()
            }
            else -> {
                // Разрешения не предоставлены
                Toast.makeText(
                    this,
                    "Необходимо разрешение на местоположение для работы приложения",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Проверка и запрос разрешений на местоположение
        checkAndRequestLocationPermissions()
        
        // Включение режима edge-to-edge для современного UI
        enableEdgeToEdge()
        
        // Установка Compose UI
        setContent {
            BuggyWeatherAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    /**
     * Проверяет наличие разрешений на местоположение и запрашивает их при необходимости.
     * Исправлено: ранее разрешения проверялись некорректно, что приводило к сбоям.
     */
    private fun checkAndRequestLocationPermissions() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasFineLocation && !hasCoarseLocation) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

/**
 * Предпросмотр для приложения.
 * Используется для визуализации в Android Studio без запуска приложения.
 */
@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    BuggyWeatherAppTheme {
        Text("Weather App Preview")
    }
}