package ru.yandex.buggyweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import ru.yandex.buggyweatherapp.ui.screens.WeatherScreen
import ru.yandex.buggyweatherapp.ui.theme.BuggyWeatherAppTheme
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    
    // ОШИБКА: ViewModel создается напрямую, не через ViewModelProvider
    private val weatherViewModel = WeatherViewModel()
    
    // ОШИБКА: Нет правильной обработки разрешений
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // ОШИБКА: Нет обработки изменений разрешений
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // ОШИБКА: Нет обработки изменений разрешений
            }
            else -> {
                // ОШИБКА: Нет обновления UI при отказе в разрешениях
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ОШИБКА: Нет обработки ошибок для отсутствующих разрешений
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
        
        enableEdgeToEdge()
        
        // ОШИБКА: Божественная активити, обрабатывающая всё
        setContent {
            BuggyWeatherAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(
                        viewModel = weatherViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    // ОШИБКА: Не обрабатываются изменения конфигурации
    
    // ОШИБКА: Утечка памяти - нет очистки ресурсов
    override fun onDestroy() {
        super.onDestroy()
        // Должны очищаться ресурсы, но этого не происходит
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    BuggyWeatherAppTheme {
        // ОШИБКА: Отсутствует реализация превью
        Text("Weather App Preview")
    }
}