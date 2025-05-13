package ru.yandex.buggyweatherapp.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
) {
    // Оптимизированная реализация toString с использованием StringBuilder
    override fun toString(): String {
        return buildString {
            append("Latitude: $latitude, Longitude: $longitude")
            name?.let {
                append(", Name: $it")
            }
        }
    }
    
    // Переопределение equals и hashCode для соответствия контракту
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }
}