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
    val timestamp: Long = System.currentTimeMillis(),
    val notamCount: Int = 0,
    val notamList: List<NotamData> = emptyList()
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

        // Дескрипторы
        val descriptorsList = listOf(
            "TS" to "гроза с", "SH" to "ливневой", "FZ" to "переохлаждённый",
            "MI" to "низкий", "PR" to "частичный", "BC" to "клочьями",
            "DR" to "низовая метель", "BL" to "метель"
        )

        var descriptor = ""
        for ((key, value) in descriptorsList) {
            if (c.startsWith(key)) {
                descriptor = value
                c = c.removePrefix(key)
                break
            }
        }

        // Основные явления - парсим ВСЕ двухбуквенные коды
        val phenomena = mapOf(
            "DZ" to "морось", "RA" to "дождь", "SN" to "снег",
            "SG" to "снежные зёрна", "IC" to "ледяные кристаллы",
            "PL" to "ледяная крупа", "GR" to "град", "GS" to "мелкий град",
            "UP" to "неопред. осадки", "FG" to "туман", "BR" to "дымка",
            "HZ" to "мгла", "FU" to "дым", "DU" to "пыль", "SA" to "песок",
            "VA" to "вулканический пепел", "PO" to "пылевые вихри",
            "SQ" to "шквал", "FC" to "смерч", "DS" to "пылевая буря", "SS" to "песчаная буря"
        )
        
        // Творительный падеж для правильного склонения
        val phenomenaInstrumental = mapOf(
            "DZ" to "моросью", "RA" to "дождём", "SN" to "снегом",
            "SG" to "снежными зёрнами", "IC" to "ледяными кристаллами",
            "PL" to "ледяной крупой", "GR" to "градом", "GS" to "мелким градом",
            "UP" to "неопред. осадками", "FG" to "туманом", "BR" to "дымкой",
            "HZ" to "мглой", "FU" to "дымом", "DU" to "пылью", "SA" to "песком",
            "VA" to "вулканическим пеплом", "PO" to "пылевыми вихрями",
            "SQ" to "шквалом", "FC" to "смерчем", "DS" to "пылевой бурей", "SS" to "песчаной бурей"
        )

        // Собираем все явления из оставшейся части
        val phenomenaList = mutableListOf<String>()
        var i = 0
        while (i + 1 < c.length) {
            val twoChar = c.substring(i, i + 2)
            phenomena[twoChar]?.let { phenomenaList.add(twoChar) }
            i += 2
        }
        
        // Если есть только одно явление - оставляем его как есть
        if (c.length == 2) {
            phenomenaList.clear()
            if (phenomena.containsKey(c)) {
                phenomenaList.add(c)
            }
        }

        // Формируем итоговую строку
        if (descriptor.isNotEmpty()) {
            result.append("$descriptor ")
        }
        
        if (phenomenaList.isNotEmpty()) {
            if (phenomenaList.size == 1) {
                // Одно явление - именительный падеж
                result.append(phenomena[phenomenaList[0]] ?: phenomenaList[0])
            } else {
                // Несколько явлений - первое в именительном, остальные в творительном через "с"
                result.append(phenomena[phenomenaList[0]] ?: phenomenaList[0])
                for (idx in 1 until phenomenaList.size) {
                    result.append(" с ")
                    result.append(phenomenaInstrumental[phenomenaList[idx]] ?: phenomena[phenomenaList[idx]] ?: phenomenaList[idx])
                }
            }
        } else if (c.isNotEmpty()) {
            result.append(phenomena[c] ?: c)
        }

        return result.toString().trim()
    }
}



