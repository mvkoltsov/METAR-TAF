package com.example.meteometar.network

import com.example.meteometar.data.AirportData
import com.example.meteometar.data.TafData
import com.example.meteometar.data.TafParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Репозиторий для получения TAF данных из Aviation Weather API
 */
object TafRepository {

    private const val AWC_BASE_URL = "https://aviationweather.gov/api/data/taf"
    private const val USER_AGENT = "Mozilla/5.0 (Android) MeteometarApp/1.0"
    private const val TIMEOUT_MS = 30000

    /**
     * Получает TAF данные для указанного списка аэропортов
     */
    suspend fun fetchTafForAirports(icaoList: List<String>): Result<Map<String, TafData>> =
        withContext(Dispatchers.IO) {
            try {
                val result = mutableMapOf<String, TafData>()

                // AWC API
                val awcResult = fetchFromAwc(icaoList)
                result.putAll(awcResult)

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Получает TAF данные из Aviation Weather Center API
     */
    private suspend fun fetchFromAwc(icaos: List<String>): Map<String, TafData> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, TafData>()

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
                    e.printStackTrace()
                }
            }

            result
        }

    /**
     * Парсит ответ от AWC API
     */
    private fun parseAwcResponse(json: String): Map<String, TafData>? {
        return try {
            val result = mutableMapOf<String, TafData>()
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val icao = obj.optString("icaoId", "")
                    .ifEmpty { obj.optString("station", "") }
                    .ifEmpty { obj.optString("stationId", "") }
                    .uppercase()

                val raw = obj.optString("rawTAF", "")
                    .ifEmpty { obj.optString("raw", "") }
                    .ifEmpty { obj.optString("raw_text", "") }

                if (icao.length == 4 && raw.isNotEmpty()) {
                    result[icao] = TafParser.parse(icao, raw, "awc")
                }
            }

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
