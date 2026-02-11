package com.example.meteometar.network

import com.example.meteometar.data.AirportData
import com.example.meteometar.data.MetarData
import com.example.meteometar.data.MetarParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Репозиторий для получения METAR данных из Aviation Weather API
 */
object MetarRepository {

    private const val AWC_BASE_URL = "https://aviationweather.gov/api/data/metar"
    private const val TGFTP_STATION_URL = "https://tgftp.nws.noaa.gov/data/observations/metar/stations/%s.TXT"

    private const val USER_AGENT = "Mozilla/5.0 (Android) MeteometarApp/1.0"
    private const val TIMEOUT_MS = 30000

    /**
     * Получает METAR данные для указанного списка аэропортов
     */
    suspend fun fetchMetarForAirports(icaoList: List<String>): Result<Map<String, MetarData>> =
        withContext(Dispatchers.IO) {
            try {
                val result = mutableMapOf<String, MetarData>()

                // Первый источник: AWC API (основной)
                val awcResult = fetchFromAwc(icaoList)
                result.putAll(awcResult)

                // Второй источник: TGFTP для отсутствующих
                val missing = icaoList.filter { it !in result }
                for (icao in missing) {
                    fetchFromTgftp(icao)?.let { result[icao] = it }
                }

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Получает METAR данные для всех казахстанских аэропортов (для обратной совместимости)
     */
    suspend fun fetchAllMetar(): Result<Map<String, MetarData>> =
        fetchMetarForAirports(AirportData.ICAO_LIST)

    /**
     * Получает METAR данные из Aviation Weather Center API
     */
    private suspend fun fetchFromAwc(icaos: List<String>): Map<String, MetarData> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, MetarData>()

            // Разбиваем на чанки по 18 ICAO кодов
            icaos.chunked(18).forEach { chunk ->
                try {
                    val ids = chunk.joinToString(",")
                    val url = URL("$AWC_BASE_URL?format=json&ids=$ids")

                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("Accept", "application/json")
                        connectTimeout = TIMEOUT_MS
                        readTimeout = TIMEOUT_MS
                    }

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        parseAwcResponse(response)?.let { result.putAll(it) }
                    }

                    connection.disconnect()
                } catch (e: Exception) {
                    // Продолжаем с следующим чанком при ошибке
                    e.printStackTrace()
                }
            }

            result
        }

    /**
     * Парсит ответ от AWC API
     */
    private fun parseAwcResponse(json: String): Map<String, MetarData>? {
        return try {
            val result = mutableMapOf<String, MetarData>()
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val icao = obj.optString("icaoId", "")
                    .ifEmpty { obj.optString("station", "") }
                    .ifEmpty { obj.optString("stationId", "") }
                    .uppercase()

                val raw = obj.optString("rawOb", "")
                    .ifEmpty { obj.optString("raw", "") }
                    .ifEmpty { obj.optString("raw_text", "") }

                if (icao.length == 4 && raw.isNotEmpty()) {
                    result[icao] = MetarParser.parse(icao, raw, "awc")
                }
            }

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Резервный источник: TGFTP
     */
    private suspend fun fetchFromTgftp(icao: String): MetarData? = withContext(Dispatchers.IO) {
        try {
            val url = URL(String.format(TGFTP_STATION_URL, icao.uppercase()))

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val lines = response.lines().filter { it.isNotBlank() }

                // Последняя строка обычно содержит METAR
                val raw = lines.lastOrNull()
                if (raw != null && raw.contains(Regex("""\b(METAR|SPECI|KT|MPS|Q\d{4}|A\d{4})\b"""))) {
                    connection.disconnect()
                    return@withContext MetarParser.parse(icao, raw, "tgftp")
                }
            }

            connection.disconnect()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

