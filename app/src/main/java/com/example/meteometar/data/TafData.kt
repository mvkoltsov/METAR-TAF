package com.example.meteometar.data

/**
 * Данные изменения прогноза (FM, TEMPO, BECMG, PROB)
 */
data class TafChange(
    val indicator: String,          // FM101200, TEMPO, BECMG, PROB30
    val indicatorText: String,      // "С 10 числа 12:00 UTC", "Временами"
    val wind: WindData? = null,
    val visibilityM: Float? = null,
    val weather: List<String> = emptyList(),
    val clouds: List<CloudLayer> = emptyList()
) {
    fun getIndicatorDisplayName(): String = when {
        indicator.startsWith("FM") -> {
            try {
                val time = indicator.substring(2)
                val day = time.substring(0, 2)
                val hour = time.substring(2, 4)
                val minute = time.substring(4, 6)
                "С $day числа $hour:$minute UTC"
            } catch (e: Exception) {
                indicatorText
            }
        }
        indicator.startsWith("PROB") -> {
            val prob = indicator.substring(4).take(2)
            "Вероятность $prob%"
        }
        indicator == "TEMPO" -> "Временами"
        indicator == "BECMG" -> "Постепенное изменение"
        else -> indicator
    }
}

/**
 * Полные данные TAF (Terminal Aerodrome Forecast)
 */
data class TafData(
    val icao: String,
    val rawTaf: String,
    val issueTime: String? = null,       // Время выпуска прогноза
    val validFrom: String? = null,       // Начало периода действия
    val validTo: String? = null,         // Конец периода действия
    val wind: WindData = WindData(),
    val visibilityM: Float? = null,
    val weather: List<String> = emptyList(),
    val clouds: List<CloudLayer> = emptyList(),
    val changes: List<TafChange> = emptyList(),
    val source: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val cityName: String get() = AirportData.getCityName(icao)

    fun getIssueTimeDisplay(): String {
        if (issueTime == null) return ""
        return try {
            // Формат: DDHHmm
            val day = issueTime.substring(0, 2)
            val hour = issueTime.substring(2, 4)
            val minute = issueTime.substring(4, 6)
            "$day числа $hour:$minute UTC"
        } catch (e: Exception) {
            issueTime
        }
    }

    fun getValidPeriodDisplay(): String {
        if (validFrom == null || validTo == null) return ""
        return try {
            // Формат: DDHH/DDHH
            val fromDay = validFrom.substring(0, 2)
            val fromHour = validFrom.substring(2, 4)
            val toDay = validTo.substring(0, 2)
            val toHour = validTo.substring(2, 4)
            "с $fromDay числа $fromHour:00 до $toDay числа $toHour:00 UTC"
        } catch (e: Exception) {
            "$validFrom — $validTo"
        }
    }

    fun getVisibilityString(): String {
        return when {
            visibilityM == null -> ""
            visibilityM >= 10000 -> "≥10 км"
            visibilityM >= 1000 -> "${(visibilityM / 1000).toInt()} км"
            else -> "${visibilityM.toInt()} м"
        }
    }

    fun getCloudsDisplay(): String {
        if (clouds.isEmpty()) return "Без облачности"
        val significant = clouds.find { it.amount in listOf("BKN", "OVC") } ?: clouds.first()
        return significant.toDisplayString()
    }

    fun getWeatherDisplay(): String {
        return weather.joinToString(", ") { translateWeatherCode(it) }
    }

    private fun translateWeatherCode(code: String): String {
        val result = StringBuilder()
        var c = code.uppercase()

        when {
            c.startsWith("+") -> { result.append("сильный "); c = c.drop(1) }
            c.startsWith("-") -> { result.append("слабый "); c = c.drop(1) }
        }

        if (c.startsWith("VC")) {
            result.append("в окрестностях ")
            c = c.drop(2)
        }

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

        val phenomena = mapOf(
            "DZ" to "морось", "RA" to "дождь", "SN" to "снег",
            "SG" to "снежные зёрна", "IC" to "ледяные кристаллы",
            "PL" to "ледяная крупа", "GR" to "град", "GS" to "мелкий град",
            "UP" to "неопред. осадки", "FG" to "туман", "BR" to "дымка",
            "HZ" to "мгла", "FU" to "дым", "DU" to "пыль", "SA" to "песок",
            "VA" to "вулканический пепел", "PO" to "пылевые вихри",
            "SQ" to "шквал", "FC" to "смерч", "DS" to "пылевая буря", "SS" to "песчаная буря"
        )

        // Собираем все явления из оставшейся части
        val phenomenaList = mutableListOf<String>()
        var i = 0
        while (i + 1 < c.length) {
            val twoChar = c.substring(i, i + 2)
            phenomena[twoChar]?.let { phenomenaList.add(it) }
            i += 2
        }

        if (c.length == 2) {
            phenomenaList.clear()
            phenomena[c]?.let { phenomenaList.add(it) }
        }

        if (descriptor.isNotEmpty()) {
            result.append("$descriptor ")
        }

        if (phenomenaList.isNotEmpty()) {
            result.append(phenomenaList.joinToString(" со "))
        } else if (c.isNotEmpty()) {
            result.append(phenomena[c] ?: c)
        }

        return result.toString().trim()
    }
}
