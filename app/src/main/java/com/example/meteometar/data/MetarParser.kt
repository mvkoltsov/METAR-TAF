package com.example.meteometar.data

/**
 * Парсер METAR данных из raw строки
 */
object MetarParser {

    // Regex для разбора ветра: dddffGggKT или dddffGggMPS
    private val WIND_REGEX = Regex("""(?:VRB|(\d{3}))(\d{2})(?:G(\d{2}))?(KT|MPS)""")

    // Regex для температуры/точки росы: TT/DD или TT/
    private val TEMP_REGEX = Regex("""\b(M?\d{2})/(M?\d{2}|//)?\b""")

    // Regex для QNH: Q1013 или A2992
    private val QNH_Q_REGEX = Regex("""\bQ(\d{4})\b""")
    private val QNH_A_REGEX = Regex("""\bA(\d{4})\b""")

    // Regex для времени: ddHHMMZ
    private val TIME_REGEX = Regex("""\b(\d{6})Z\b""")

    // Regex для видимости (метры): 9999, 4000, etc
    private val VIS_M_REGEX = Regex("""\b(9999|\d{4})\b""")

    // Regex для облачности: FEW015, SCT020, BKN030, OVC040
    private val CLOUD_REGEX = Regex("""\b(FEW|SCT|BKN|OVC)(\d{3})\b""")

    // Regex для погодных явлений
    private val WX_REGEX = Regex("""(?:\+|-|VC)?(?:(?:MI|PR|BC|DR|BL|SH|TS|FZ)?(?:DZ|RA|SN|SG|IC|PL|GR|GS|UP|FG|BR|HZ|FU|DU|SA|VA|PO|SQ|FC|SS|DS))""")

    /**
     * Парсит raw METAR строку и возвращает MetarData
     */
    fun parse(icao: String, raw: String, source: String): MetarData {
        val upperRaw = raw.uppercase()

        val wind = parseWind(upperRaw)
        val tempDew = parseTemperature(upperRaw)
        val qnh = parseQnh(upperRaw)
        val time = parseTime(upperRaw)
        val visibility = parseVisibility(upperRaw)
        val clouds = parseClouds(upperRaw)
        val weather = parseWeather(upperRaw)
        val category = deriveCategory(visibility, clouds)

        return MetarData(
            icao = icao.uppercase(),
            time = time,
            flightCategory = category,
            wind = wind,
            visibilityM = visibility,
            clouds = clouds,
            weather = weather,
            tempC = tempDew.first,
            dewpointC = tempDew.second,
            qnhHpa = qnh,
            rawMetar = raw,
            source = source
        )
    }

    private fun parseWind(raw: String): WindData {
        val match = WIND_REGEX.find(raw) ?: return WindData()

        val dirStr = match.groupValues[1]
        val spdStr = match.groupValues[2]
        val gustStr = match.groupValues[3]
        val unit = match.groupValues[4]

        val dir = if (dirStr.isEmpty()) null else dirStr.toIntOrNull()
        var spd = spdStr.toIntOrNull() ?: 0
        var gust = if (gustStr.isEmpty()) null else gustStr.toIntOrNull()

        // Конвертация MPS в KT
        if (unit == "MPS") {
            spd = (spd * 1.943844).toInt()
            gust = gust?.let { (it * 1.943844).toInt() }
        }

        return WindData(
            directionDeg = dir,
            speedKt = spd,
            gustKt = gust
        )
    }

    private fun parseTemperature(raw: String): Pair<Float?, Float?> {
        val match = TEMP_REGEX.find(raw) ?: return Pair(null, null)

        val tempStr = match.groupValues[1]
        val dewStr = match.groupValues[2]

        val temp = parseTemp(tempStr)
        val dew = if (dewStr.isNotEmpty() && dewStr != "//") parseTemp(dewStr) else null

        return Pair(temp, dew)
    }

    private fun parseTemp(str: String): Float? {
        if (str.isEmpty() || str == "//") return null
        return if (str.startsWith("M")) {
            -str.substring(1).toFloatOrNull()!!
        } else {
            str.toFloatOrNull()
        }
    }

    private fun parseQnh(raw: String): Float? {
        // Сначала ищем Q (hPa)
        QNH_Q_REGEX.find(raw)?.let {
            return it.groupValues[1].toFloatOrNull()
        }

        // Затем A (inHg)
        QNH_A_REGEX.find(raw)?.let {
            val inHg = (it.groupValues[1].toFloatOrNull() ?: return null) / 100f
            return inHg * 33.8638866667f
        }

        return null
    }

    private fun parseTime(raw: String): String? {
        val match = TIME_REGEX.find(raw) ?: return null
        return match.groupValues[1]
    }

    private fun parseVisibility(raw: String): Float? {
        if (raw.contains("CAVOK")) return 10000f

        val match = VIS_M_REGEX.find(raw) ?: return null
        return match.groupValues[1].toFloatOrNull()
    }

    private fun parseClouds(raw: String): List<CloudLayer> {
        return CLOUD_REGEX.findAll(raw).map { match ->
            CloudLayer(
                amount = match.groupValues[1],
                baseFt = match.groupValues[2].toInt() * 100
            )
        }.toList()
    }

    private fun parseWeather(raw: String): List<String> {
        return WX_REGEX.findAll(raw).map { it.value }.toList()
    }

    /**
     * Определение категории полёта по видимости и облачности
     */
    private fun deriveCategory(visibilityM: Float?, clouds: List<CloudLayer>): FlightCategory {
        // Нижняя граница облачности (ceiling) - BKN или OVC
        val ceilingFt = clouds.find { it.amount in listOf("BKN", "OVC") }?.baseFt

        // LIFR: vis < 1500m или ceiling < 1000ft
        if ((visibilityM != null && visibilityM < 1500) ||
            (ceilingFt != null && ceilingFt < 1000)) {
            return FlightCategory.LIFR
        }

        // IFR: vis < 5000m или ceiling < 3000ft
        if ((visibilityM != null && visibilityM < 5000) ||
            (ceilingFt != null && ceilingFt < 3000)) {
            return FlightCategory.IFR
        }

        // MVFR: vis < 8000m или ceiling < 5000ft
        if ((visibilityM != null && visibilityM < 8000) ||
            (ceilingFt != null && ceilingFt < 5000)) {
            return FlightCategory.MVFR
        }

        return FlightCategory.VFR
    }
}

