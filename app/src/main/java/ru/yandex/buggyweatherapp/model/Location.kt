package ru.yandex.buggyweatherapp.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
) {
    // ОШИБКА: Неэффективные операции со строками, которые можно оптимизировать
    override fun toString(): String {
        var result = ""
        result += "Latitude: $latitude, "
        result += "Longitude: $longitude"
        name?.let {
            result += ", Name: $it"
        }
        return result
    }
    
    // ОШИБКА: Неправильная реализация equals, которая не соответствует hashCode
    override fun equals(other: Any?): Boolean {
        if (other !is Location) return false
        return latitude == other.latitude && longitude == other.longitude
    }
}