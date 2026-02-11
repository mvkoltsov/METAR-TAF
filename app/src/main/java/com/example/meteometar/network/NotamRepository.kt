package com.example.meteometar.network

import android.util.Log
import com.example.meteometar.data.NotamCategory
import com.example.meteometar.data.NotamData
import com.example.meteometar.data.NotamDecoder
import com.example.meteometar.data.NotamType
import com.example.meteometar.data.DecodedNotam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Репозиторий для получения NOTAM данных
 * Парсинг с нескольких источников
 */
object NotamRepository {

    private const val TAG = "NotamRepository"

    // Кэш NOTAM
    private var cachedNotams: Map<String, List<NotamData>> = emptyMap()
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION = 5 * 60 * 1000L // 5 минут

    /**
     * Получить NOTAM для аэродрома
     */
    suspend fun fetchNotamForAirport(icao: String): Result<List<NotamData>> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastFetchTime < CACHE_DURATION && cachedNotams.isNotEmpty()) {
                val cached = cachedNotams[icao.uppercase()] ?: emptyList()
                return@withContext Result.success(cached)
            }

            val notams = fetchNotamsForIcao(icao)
            if (notams.isNotEmpty()) {
                val updatedCache = cachedNotams.toMutableMap()
                updatedCache[icao.uppercase()] = notams
                cachedNotams = updatedCache
                lastFetchTime = now
            }

            Result.success(notams)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching NOTAM for $icao", e)
            Result.success(emptyList())
        }
    }

    /**
     * Получить NOTAM для списка аэродромов
     */
    suspend fun fetchNotamForAirports(icaoList: List<String>): Result<Map<String, List<NotamData>>> =
        withContext(Dispatchers.IO) {
            try {
                val result = mutableMapOf<String, List<NotamData>>()
                val now = System.currentTimeMillis()

                for (icao in icaoList) {
                    val cached = if (now - lastFetchTime < CACHE_DURATION) {
                        cachedNotams[icao.uppercase()]
                    } else null

                    if (cached != null) {
                        result[icao] = cached
                    } else {
                        val notams = fetchNotamsForIcao(icao)
                        if (notams.isNotEmpty()) {
                            result[icao] = notams
                            val updatedCache = cachedNotams.toMutableMap()
                            updatedCache[icao.uppercase()] = notams
                            cachedNotams = updatedCache
                        }
                    }
                }

                if (result.isNotEmpty()) {
                    lastFetchTime = now
                }

                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching NOTAMs", e)
                Result.success(emptyMap())
            }
        }

    /**
     * Загрузка NOTAM для конкретного ICAO
     */
    private fun fetchNotamsForIcao(icao: String): List<NotamData> {
        // Источник 1: AviationWeather.gov
        try {
            val awNotams = fetchFromAviationWeather(icao)
            if (awNotams.isNotEmpty()) {
                Log.d(TAG, "Got ${awNotams.size} NOTAMs from AviationWeather for $icao")
                return awNotams
            }
        } catch (e: Exception) {
            Log.w(TAG, "AviationWeather failed for $icao: ${e.message}")
        }

        // Источник 2: OurAirports
        try {
            val oaNotams = fetchFromOurAirports(icao)
            if (oaNotams.isNotEmpty()) {
                Log.d(TAG, "Got ${oaNotams.size} NOTAMs from OurAirports for $icao")
                return oaNotams
            }
        } catch (e: Exception) {
            Log.w(TAG, "OurAirports failed for $icao: ${e.message}")
        }

        return emptyList()
    }

    /**
     * Получение NOTAM с AviationWeather.gov
     */
    private fun fetchFromAviationWeather(icao: String): List<NotamData> {
        val url = URL("https://aviationweather.gov/api/data/notam?ids=$icao&format=raw")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "MeteoMetar/2.0")

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "AviationWeather response for $icao: ${response.take(500)}")
            if (response.isNotBlank()) {
                return parseRawNotam(response, icao)
            }
        }
        return emptyList()
    }

    /**
     * Получение NOTAM с OurAirports
     */
    private fun fetchFromOurAirports(icao: String): List<NotamData> {
        val url = URL("https://ourairports.com/airports/$icao/notam.html")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "MeteoMetar/2.0")

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parseNotamHtml(response, icao)
        }
        return emptyList()
    }

    /**
     * Парсинг сырого текста NOTAM
     */
    private fun parseRawNotam(response: String, defaultIcao: String): List<NotamData> {
        val notams = mutableListOf<NotamData>()

        if (response.isBlank()) return notams

        // Паттерн 1: Стандартный формат NOTAM
        val notamPattern = Regex(
            "([A-Z]\\d{4}/\\d{2})\\s*NOTAM([NRC])(.+?)(?=[A-Z]\\d{4}/\\d{2}\\s*NOTAM|$)",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        for (match in notamPattern.findAll(response)) {
            try {
                val notam = parseNotamBlock(match.groupValues[1], match.groupValues[2], match.groupValues[3], defaultIcao)
                if (notam != null) {
                    notams.add(notam)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse NOTAM: ${e.message}")
            }
        }

        // Если не нашли стандартные NOTAM, ищем по Q-строке
        if (notams.isEmpty()) {
            val qPattern = Regex(
                "Q\\)\\s*([A-Z]{4})/([A-Z]{5})/[^/]*/[^/]*/[^/]*/\\d+/\\d+/[^\\s]+",
                RegexOption.IGNORE_CASE
            )

            val blocks = response.split(Regex("(?=Q\\)\\s*[A-Z]{4}/)"))

            for (block in blocks) {
                if (block.isBlank()) continue

                try {
                    val qMatch = qPattern.find(block)
                    if (qMatch != null) {
                        val fir = qMatch.groupValues[1]
                        val qCode = qMatch.groupValues[2]

                        val aPattern = Regex("A\\)\\s*([A-Z]{4})")
                        val aMatch = aPattern.find(block)
                        val icao = aMatch?.groupValues?.get(1) ?: fir

                        if (icao.uppercase() != defaultIcao.uppercase() && fir.uppercase() != defaultIcao.uppercase()) continue

                        val idPattern = Regex("([A-Z]\\d{4}/\\d{2})")
                        val idMatch = idPattern.find(block)
                        val id = idMatch?.groupValues?.get(1) ?: "N${System.currentTimeMillis() % 10000}/25"

                        val bPattern = Regex("B\\)\\s*(\\d{10,12})")
                        val bMatch = bPattern.find(block)
                        val validFrom = bMatch?.groupValues?.get(1)?.let { NotamDecoder.parseNotamDateTime(it) } ?: ""

                        val cPattern = Regex("C\\)\\s*(\\d{10,12}|PERM|EST)")
                        val cMatch = cPattern.find(block)
                        val cValue = cMatch?.groupValues?.get(1) ?: ""
                        val isPermanent = cValue == "PERM"
                        val validTo = when {
                            isPermanent -> "Постоянно"
                            cValue == "EST" -> "Уточняется"
                            cValue.isNotEmpty() -> NotamDecoder.parseNotamDateTime(cValue)
                            else -> ""
                        }

                        val category = NotamDecoder.determineCategory(qCode, block)
                        val decoded = NotamDecoder.decode(block, qCode)

                        notams.add(NotamData(
                            id = id,
                            icao = icao,
                            type = NotamType.NEW,
                            category = category,
                            rawText = block.trim().take(1000),
                            effectiveFrom = validFrom,
                            effectiveTo = validTo,
                            isPermanent = isPermanent,
                            qCode = qCode,
                            decoded = decoded
                        ))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse NOTAM block: ${e.message}")
                }
            }
        }

        return notams
    }

    /**
     * Парсинг HTML страницы с NOTAM
     */
    private fun parseNotamHtml(html: String, defaultIcao: String): List<NotamData> {
        val notams = mutableListOf<NotamData>()

        val cleanedHtml = html
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")

        val notamPattern = Regex(
            "([A-Z]\\d{4}/\\d{2})\\s*NOTAM([NRC])(.+?)(?=[A-Z]\\d{4}/\\d{2}\\s*NOTAM|$)",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        for (match in notamPattern.findAll(cleanedHtml)) {
            try {
                val notam = parseNotamBlock(match.groupValues[1], match.groupValues[2], match.groupValues[3], defaultIcao)
                if (notam != null) {
                    notams.add(notam)
                }
            } catch (e: Exception) {
                // Пропускаем
            }
        }

        return notams
    }

    private fun parseNotamBlock(id: String, typeChar: String, content: String, defaultIcao: String): NotamData? {
        val fullText = "$id NOTAM$typeChar$content"
        val type = when (typeChar.uppercase()) {
            "N" -> NotamType.NEW
            "R" -> NotamType.REPLACE
            "C" -> NotamType.CANCEL
            else -> NotamType.NEW
        }

        val qPattern = Regex("Q\\)\\s*([A-Z]{4})/(Q[A-Z]{4})")
        val qMatch = qPattern.find(content)
        val qCode = qMatch?.groupValues?.get(2)
        val firCode = qMatch?.groupValues?.get(1)

        val aPattern = Regex("A\\)\\s*([A-Z]{4})")
        val aMatch = aPattern.find(content)
        val icao = aMatch?.groupValues?.get(1) ?: firCode ?: defaultIcao

        val bPattern = Regex("B\\)\\s*(\\d{10,12})")
        val bMatch = bPattern.find(content)
        val validFrom = bMatch?.groupValues?.get(1)?.let { NotamDecoder.parseNotamDateTime(it) } ?: ""

        val cPattern = Regex("C\\)\\s*(\\d{10,12}|PERM|EST)")
        val cMatch = cPattern.find(content)
        val cValue = cMatch?.groupValues?.get(1) ?: ""
        val isPermanent = cValue == "PERM"
        val validTo = when {
            isPermanent -> "Постоянно"
            cValue == "EST" -> "Уточняется"
            cValue.isNotEmpty() -> NotamDecoder.parseNotamDateTime(cValue)
            else -> ""
        }

        val dPattern = Regex("D\\)\\s*([^E)]+)")
        val dMatch = dPattern.find(content)
        val schedule = dMatch?.groupValues?.get(1)?.trim()

        val category = NotamDecoder.determineCategory(qCode, fullText)
        val decoded = NotamDecoder.decode(fullText, qCode)

        return NotamData(
            id = id,
            icao = icao,
            type = type,
            category = category,
            rawText = fullText.trim(),
            effectiveFrom = validFrom,
            effectiveTo = validTo,
            isPermanent = isPermanent,
            schedule = schedule,
            qCode = qCode,
            decoded = decoded
        )
    }
}
