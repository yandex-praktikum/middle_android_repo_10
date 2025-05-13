package ru.yandex.buggyweatherapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ru.yandex.buggyweatherapp.R
import ru.yandex.buggyweatherapp.model.WeatherUiModel

/**
 * Отображает подробную карточку погоды с использованием современных Compose подходов:
 * - AsyncImage для асинхронной загрузки изображений
 * - Строковые ресурсы для интернационализации
 * - Семантические свойства для доступности
 * - Форматирование данных на уровне UI, а не ViewModel
 * - Оптимизированное отображение элементов без избыточной перерисовки
 */
@Composable
fun DetailedWeatherCard(
    weather: WeatherUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с названием города и кнопкой избранного
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weather.cityName,
                    style = MaterialTheme.typography.headlineMedium
                )
// Кнопка избранного удалена
            }
            
            // Температура и иконка погоды
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Используем AsyncImage с Coil вместо AndroidView с ImageView
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://openweathermap.org/img/wn/${weather.icon}@2x.png")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Weather icon for ${weather.description}",
                    modifier = Modifier.size(50.dp)
                )
                
                // Форматирование температуры с использованием строковых ресурсов
                Text(
                    text = stringResource(R.string.format_temperature, weather.temperature.toInt()),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            
            // Описание погоды
            Text(
                text = weather.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Используем обычный Column вместо LazyColumn для маленького списка
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WeatherDataRow(
                    label = stringResource(R.string.feels_like),
                    value = stringResource(R.string.format_temperature, weather.feelsLike.toInt())
                )

                WeatherDataRow(
                    label = "Min/Max",
                    value = "${stringResource(R.string.format_temperature, weather.minTemp.toInt())} / ${stringResource(R.string.format_temperature, weather.maxTemp.toInt())}"
                )

                WeatherDataRow(
                    label = stringResource(R.string.humidity),
                    value = stringResource(R.string.format_humidity, weather.humidity)
                )

                WeatherDataRow(
                    label = "Pressure",
                    value = stringResource(R.string.format_pressure, weather.pressure)
                )

                WeatherDataRow(
                    label = stringResource(R.string.wind),
                    value = stringResource(R.string.format_wind_speed, weather.windSpeed)
                )

                // Получим шаблон форматирования до создания функции
                val timePattern = stringResource(R.string.format_time)
                
                // Функция форматирования, использующая предварительно полученный шаблон
                fun formatTime(timestamp: Long): String {
                    val date = Date(timestamp * 1000)
                    return SimpleDateFormat(timePattern, Locale.getDefault()).format(date)
                }

                WeatherDataRow(
                    label = stringResource(R.string.sunrise),
                    value = formatTime(weather.sunriseTime)
                )

                WeatherDataRow(
                    label = stringResource(R.string.sunset),
                    value = formatTime(weather.sunsetTime)
                )
            }
        }
    }
}

@Composable
private fun WeatherDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}