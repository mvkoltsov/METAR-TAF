package com.example.meteometar.data

/**
 * Парсер TAF данных из raw строки
 */
object TafParser {

    // Regex для разбора ветра
    private val WIND_REGEX = Regex("""(?:VRB|(\d{3}))(\d{2,3})(?:G(\d{2,3}))?(KT|MPS)""")

    // Regex для времени выпуска
    private val ISSUE_TIME_REGEX = Regex("""\b(\d{6})Z\b""")

    // Regex для периода действия
    private val VALID_PERIOD_REGEX = Regex("""\b(\d{4})/(\d{4})\b""")

    // Regex для видимости (метры)
    private val VIS_M_REGEX = Regex("""\b(9999|\d{4})\b""")

    // Regex для облачности
    private val CLOUD_REGEX = Regex("""\b(FEW|SCT|BKN|OVC|VV)(\d{3})(CB|TCU)?\b""")

    // Regex для погодных явлений
    private val WX_REGEX = Regex("""(?:\+|-|VC)?(?:(?:MI|PR|BC|DR|BL|SH|TS|FZ)?(?:DZ|RA|SN|SG|IC|PL|GR|GS|UP|FG|BR|HZ|FU|DU|SA|VA|PO|SQ|FC|SS|DS))""")

    // Regex для изменений FM
    private val FM_REGEX = Regex("""FM(\d{6})""")

    // Regex для TEMPO/BECMG с периодом
    private val CHANGE_REGEX = Regex("""(TEMPO|BECMG|PROB\d{2}\s*TEMPO|PROB\d{2})(?:\s+(\d{4})/(\d{4}))?""")

    /**
     * Парсит raw TAF строку
     */
    fun parse(icao: String, raw: String, source: String): TafData {
        val upperRaw = raw.uppercase().replace("\n", " ").replace("  ", " ")

        // Парсим заголовок
        val issueTime = parseIssueTime(upperRaw)
        val (validFrom, validTo) = parseValidPeriod(upperRaw)

        // Находим основную часть (до первого FM/TEMPO/BECMG)
        val mainPart = getMainPart(upperRaw)

        val wind = parseWind(mainPart)
        val visibility = parseVisibility(mainPart)
        val clouds = parseClouds(mainPart)
        val weather = parseWeather(mainPart)

        // Парсим изменения
        val changes = parseChanges(upperRaw)

        return TafData(
            icao = icao.uppercase(),
            rawTaf = raw,
            issueTime = issueTime,
            validFrom = validFrom,
            validTo = validTo,
            wind = wind,
            visibilityM = visibility,
            weather = weather,
            clouds = clouds,
            changes = changes,
            source = source
        )
    }

    private fun getMainPart(raw: String): String {
        // Находим первый индикатор изменений
        val fmIndex = raw.indexOf(" FM")
        val tempoIndex = raw.indexOf(" TEMPO")
        val becmgIndex = raw.indexOf(" BECMG")
        val probIndex = raw.indexOf(" PROB")

        val indices = listOf(fmIndex, tempoIndex, becmgIndex, probIndex)
            .filter { it > 0 }

        return if (indices.isNotEmpty()) {
            raw.substring(0, indices.min())
        } else {
            raw
        }
    }

    private fun parseIssueTime(raw: String): String? {
        val match = ISSUE_TIME_REGEX.find(raw) ?: return null
        return match.groupValues[1]
    }

    private fun parseValidPeriod(raw: String): Pair<String?, String?> {
        val match = VALID_PERIOD_REGEX.find(raw) ?: return Pair(null, null)
        return Pair(match.groupValues[1], match.groupValues[2])
    }

    private fun parseWind(raw: String): WindData {
        // Штиль
        if (raw.contains("00000KT") || raw.contains("00000MPS")) {
            return WindData(directionDeg = 0, speedKt = 0)
        }

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

    private fun parseChanges(raw: String): List<TafChange> {
        val changes = mutableListOf<TafChange>()

        // Парсим FM изменения
        val fmMatches = Regex("""FM(\d{6})\s+([^F]*?)(?=FM\d{6}|TEMPO|BECMG|PROB|$)""").findAll(raw)
        for (match in fmMatches) {
            val indicator = "FM${match.groupValues[1]}"
            val content = match.groupValues[2].trim()

            changes.add(TafChange(
                indicator = indicator,
                indicatorText = formatFmIndicator(match.groupValues[1]),
                wind = parseWind(content).takeIf { it.speedKt > 0 },
                visibilityM = parseVisibility(content),
                weather = parseWeather(content),
                clouds = parseClouds(content)
            ))
        }

        // Парсим TEMPO/BECMG
        val changeMatches = Regex("""(TEMPO|BECMG|PROB\d{2}(?:\s+TEMPO)?)\s+(?:(\d{4})/(\d{4})\s+)?([^T^B^P^F]*?)(?=TEMPO|BECMG|PROB|FM\d{6}|$)""").findAll(raw)
        for (match in changeMatches) {
            val indicator = match.groupValues[1].trim()
            val content = match.groupValues[4].trim()

            changes.add(TafChange(
                indicator = indicator,
                indicatorText = formatChangeIndicator(indicator),
                wind = parseWind(content).takeIf { it.speedKt > 0 },
                visibilityM = parseVisibility(content),
                weather = parseWeather(content),
                clouds = parseClouds(content)
            ))
        }

        return changes.distinctBy { it.indicator }
    }

    private fun formatFmIndicator(time: String): String {
        return try {
            val day = time.substring(0, 2)
            val hour = time.substring(2, 4)
            val minute = time.substring(4, 6)
            "С $day числа $hour:$minute UTC"
        } catch (e: Exception) {
            "С $time"
        }
    }

    private fun formatChangeIndicator(indicator: String): String {
        return when {
            indicator == "TEMPO" -> "Временами"
            indicator == "BECMG" -> "Постепенное изменение"
            indicator.startsWith("PROB") -> {
                val prob = indicator.filter { it.isDigit() }.take(2)
                if (indicator.contains("TEMPO")) {
                    "Вероятность $prob% временами"
                } else {
                    "Вероятность $prob%"
                }
            }
            else -> indicator
        }
    }
}
