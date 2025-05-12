package ru.yandex.buggyweatherapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

// ОШИБКА: Объект со статической ссылкой на контекст (утечка памяти)
object ImageLoader {
    // ОШИБКА: Статическая ссылка на контекст, которая может привести к утечкам памяти
    private lateinit var appContext: Context
    
    // ОШИБКА: Отсутствует политика очистки кэша
    private val imageCache = ConcurrentHashMap<String, Bitmap>()
    
    // ОШИБКА: Статические ссылки на View (утечка памяти)
    private val viewReferences = HashMap<String, ImageView>()
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    // ОШИБКА: Сетевая операция без правильной обработки ошибок
    suspend fun loadImage(url: String): Bitmap? {
        return if (imageCache.containsKey(url)) {
            imageCache[url]
        } else {
            try {
                // ОШИБКА: Использование URL.openStream() вместо OkHttp или другой подходящей сетевой библиотеки
                val bitmap = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    
                    val inputStream = connection.getInputStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    // ОШИБКА: Хранение потенциально больших изображений в памяти без проверки размера
                    imageCache[url] = bitmap
                    bitmap
                }
                bitmap
            } catch (e: Exception) {
                // ОШИБКА: Проглатывание исключений
                null
            }
        }
    }
    
    // ОШИБКА: Синхронная сетевая операция в главном потоке
    fun loadImageSync(url: String): Bitmap? {
        return if (imageCache.containsKey(url)) {
            imageCache[url]
        } else {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                imageCache[url] = bitmap
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // ОШИБКА: Хранение сильной ссылки на ImageView
    fun loadInto(url: String, imageView: ImageView) {
        viewReferences[url] = imageView
        
        if (imageCache.containsKey(url)) {
            imageView.setImageBitmap(imageCache[url])
        } else {
            // ОШИБКА: Создание потока без правильного управления
            Thread {
                val bitmap = loadImageSync(url)
                
                // ОШИБКА: Доступ к imageView из фонового потока
                imageView.post {
                    imageView.setImageBitmap(bitmap)
                }
            }.start()
        }
    }
    
    // ОШИБКА: Отсутствует метод для очистки кэша или ссылок
}