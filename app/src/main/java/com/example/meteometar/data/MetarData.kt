package com.example.meteometar.data

import java.util.Locale

/**
 * Категория полёта на основе видимости и облачности
 */
enum class FlightCategory(val displayName: String) {
    VFR("ПВП"),      // Visual Flight Rules
    MVFR("Огр.ПВП"), // Marginal VFR
    IFR("ППП"),      // Instrument Flight Rules
    LIFR("Пониж.ППП") // Low IFR
}

/**
 * Данные о ветре
 */
data class WindData(
    val directionDeg: Int? = null,  // null для VRB (переменный)
    val speedKt: Int = 0,
    val gustKt: Int? = null
) {
    val speedMs: Int get() = (speedKt * 0.514444).toInt()
    val gustMs: Int? get() = gustKt?.let { (it * 0.514444).toInt() }

    fun toDisplayString(): String {
        val dir = directionDeg?.let { String.format(Locale.US, "%03d°", it) } ?: "VRB"
        val spd = "$speedMs м/с"
        val gust = gustMs?.let { " пор. $it м/с" } ?: ""
        return "$dir/$spd$gust"
    }
}

/**
 * Данные об облачности
 */
data class CloudLayer(
    val amount: String,   // FEW, SCT, BKN, OVC
    val baseFt: Int
) {
    val baseMeters: Int get() = (baseFt * 0.3048).toInt()

    fun getAmountName(): String = when (amount) {
        "FEW" -> "Небольшая"
        "SCT" -> "Рассеянная"
        "BKN" -> "Значительная"
        "OVC" -> "Сплошная"
        else -> amount
    }

    fun toDisplayString(): String {
        val baseRounded = (baseMeters / 100) * 100
        return "${getAmountName()} ≈$baseRounded м"
    }
}

/**
 * Полные данные METAR
 */
data class MetarData(
    val icao: String,
    val time: String? = null,
    val flightCategory: FlightCategory = FlightCategory.VFR,
    val wind: WindData = WindData(),
    val visibilityM: Float? = null,
    val clouds: List<CloudLayer> = emptyList(),
    val weather: List<String> = emptyList(),
    val tempC: Float? = null,
    val dewpointC: Float? = null,
    val qnhHpa: Float? = null,
    val rawMetar: String? = null,
    val source: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val cityName: String get() = AirportData.getCityName(icao)

    val qnhMmHg: Int? get() = qnhHpa?.let { (it * 0.75006).toInt() }

    fun getVisibilityString(): String {
        return when {
            visibilityM == null -> ""
            visibilityM >= 10000 -> "≥10 км"
            else -> "${visibilityM.toInt()} м"
        }
    }

    fun getTimeDisplay(): String {
        if (time == null) return ""
        // Формат времени: DDHHMM
        return try {
            val hh = time.substring(2, 4)
            val mm = time.substring(4, 6)
            "$hh:$mm UTC"
        } catch (e: Exception) {
            time
        }
    }

    fun getCloudsDisplay(): String {
        if (clouds.isEmpty()) return ""
        // Приоритет: BKN/OVC, иначе первый слой
        val significant = clouds.find { it.amount in listOf("BKN", "OVC") } ?: clouds.first()
        return significant.toDisplayString()
    }

    fun getWeatherDisplay(): String {
        return weather.joinToString(", ") { translateWeatherCode(it) }
    }

    private fun translateWeatherCode(code: String): String {
        val result = StringBuilder()
        var c = code.uppercase()

        // Интенсивность
        when {
            c.startsWith("+") -> { result.append("сильный "); c = c.drop(1) }
            c.startsWith("-") -> { result.append("слабый "); c = c.drop(1) }
        }

        // В окрестностях
        if (c.startsWith("VC")) {
            result.append("в окрестностях ")
            c = c.drop(2)
        }

        // Дескрипторы (проверяем по порядку, начиная с более длинных)
        val descriptorsList = listOf(
            "TS" to "гроза", "SH" to "ливневый", "FZ" to "переохлаждённый",
            "MI" to "низкий", "PR" to "частичный", "BC" to "клочьями",
            "DR" to "низовая метель", "BL" to "метель"
        )

        for ((key, value) in descriptorsList) {
            if (c.startsWith(key)) {
                result.append("$value ")
                c = c.removePrefix(key)
                break  // Берём только первый найденный дескриптор
            }
        }

        // Основные явления (проверяем точное совпадение оставшейся части)
        val phenomena = mapOf(
            "DZ" to "морось", "RA" to "дождь", "SN" to "снег",
            "SG" to "снежные зёрна", "IC" to "ледяные кристаллы",
            "PL" to "ледяная крупа", "GR" to "град", "GS" to "мелкий град",
            "UP" to "неопред. осадки", "FG" to "туман", "BR" to "дымка",
            "HZ" to "мгла", "FU" to "дым", "DU" to "пыль", "SA" to "песок",
            "VA" to "вулканический пепел", "PO" to "пылевые вихри",
            "SQ" to "шквал", "FC" to "смерч", "DS" to "пылевая буря", "SS" to "песчаная буря"
        )

        result.append(phenomena[c] ?: c)
        return result.toString().trim()
    }
}



