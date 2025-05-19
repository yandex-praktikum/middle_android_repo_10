package ru.yandex.buggyweatherapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

/**
 * Главный класс приложения.
 * 
 * Изменения:
 * 1. Добавлена аннотация @HiltAndroidApp для поддержки Hilt Dependency Injection
 * 2. Реализован интерфейс ImageLoaderFactory для централизованной настройки Coil
 * 3. Удалены статичные ссылки на контекст, которые вызывали утечки памяти
 * 4. Настроено кэширование изображений в памяти и на диске
 */
@HiltAndroidApp
class WeatherApplication : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
    }
    
    /**
     * Создает экземпляр ImageLoader с оптимизированными настройками кэширования.
     * Заменяет ранее используемый небезопасный класс ImageLoader.
     * 
     * @return Настроенный экземпляр ImageLoader для загрузки и кэширования изображений
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Использование 25% доступной памяти для кэша
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Использование 2% доступного места на диске
                    .build()
            }
            .respectCacheHeaders(false) // Игнорировать заголовки кэширования с сервера
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}