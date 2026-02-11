package com.example.meteometar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meteometar.data.AirportData
import com.example.meteometar.data.Country
import com.example.meteometar.data.MetarData
import com.example.meteometar.data.SettingsManager
import com.example.meteometar.data.SortOption
import com.example.meteometar.network.MetarRepository
import com.example.meteometar.network.NotamRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Состояние UI
 */
data class MetarUiState(
    val metarList: List<MetarData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0,
    val searchQuery: String = "",
    val selectedCountries: Set<String> = setOf("KZ"),
    val sortOption: SortOption = SortOption.BY_CITY,
    val favorites: Set<String> = emptySet(),
    val countries: List<Country> = AirportData.getAllCountries()
)

/**
 * ViewModel для управления METAR данными
 */
class MetarViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager.getInstance(application)

    private val _uiState = MutableStateFlow(MetarUiState())
    val uiState: StateFlow<MetarUiState> = _uiState.asStateFlow()

    // Все загруженные данные
    private var allMetarData: Map<String, MetarData> = emptyMap()

    // Интервал обновления в миллисекундах (3 минуты)
    private val updateIntervalMs = 3 * 60 * 1000L

    init {
        // Подписываемся на изменения настроек
        viewModelScope.launch {
            combine(
                settingsManager.selectedCountries,
                settingsManager.sortOption,
                settingsManager.favorites
            ) { countries, sort, favs ->
                Triple(countries, sort, favs)
            }.collect { (countries, sort, favs) ->
                _uiState.value = _uiState.value.copy(
                    selectedCountries = countries,
                    sortOption = sort,
                    favorites = favs
                )
                // Перезагружаем данные при смене стран
                if (allMetarData.isEmpty() ||
                    _uiState.value.selectedCountries != countries) {
                    loadMetar()
                } else {
                    applyFilter()
                }
            }
        }

        startAutoRefresh()
    }

    /**
     * Загружает METAR данные
     */
    fun loadMetar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Получаем аэродромы выбранных стран
            val countryIcaoList = AirportData.getIcaoListForCountries(_uiState.value.selectedCountries)

            // Добавляем избранные аэродромы (чтобы они загружались всегда)
            val favorites = _uiState.value.favorites
            val allIcaoList = (countryIcaoList + favorites).distinct()

            MetarRepository.fetchMetarForAirports(allIcaoList)
                .onSuccess { data ->
                    allMetarData = data

                    // Загружаем NOTAM для всех аэродромов
                    loadNotamForAirports(allIcaoList)

                    applyFilter()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ошибка загрузки: ${e.message}"
                    )
                }
        }
    }

    /**
     * Загружает NOTAM для списка аэродромов
     */
    private fun loadNotamForAirports(icaoList: List<String>) {
        viewModelScope.launch {
            NotamRepository.fetchNotamForAirports(icaoList)
                .onSuccess { notamMap: Map<String, List<com.example.meteometar.data.NotamData>> ->
                    // Обновляем MetarData с NOTAM
                    val updatedMetarData = allMetarData.mapValues { (icao: String, metar: MetarData) ->
                        val notams = notamMap[icao] ?: emptyList()
                        metar.copy(
                            notamCount = notams.size,
                            notamList = notams
                        )
                    }
                    allMetarData = updatedMetarData
                    applyFilter()
                }
        }
    }

    /**
     * Обновляет поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    /**
     * Переключить страну
     */
    fun toggleCountry(countryCode: String) {
        settingsManager.toggleCountry(countryCode)
    }

    /**
     * Установить вариант сортировки
     */
    fun setSortOption(option: SortOption) {
        settingsManager.setSortOption(option)
    }

    /**
     * Переключить избранное
     */
    fun toggleFavorite(icao: String) {
        settingsManager.toggleFavorite(icao)
    }

    /**
     * Применяет фильтр и сортировку к данным
     */
    private fun applyFilter() {
        val query = _uiState.value.searchQuery.lowercase().trim()
        val sortOption = _uiState.value.sortOption
        val favorites = _uiState.value.favorites

        var filtered = if (query.isEmpty()) {
            allMetarData.values.toList()
        } else {
            allMetarData.values.filter { metar ->
                metar.icao.lowercase().contains(query) ||
                metar.cityName.lowercase().contains(query) ||
                metar.getWeatherDisplay().lowercase().contains(query) ||
                metar.getCloudsDisplay().lowercase().contains(query) ||
                metar.flightCategory.name.lowercase().contains(query) ||
                metar.flightCategory.displayName.lowercase().contains(query) ||
                (AirportData.getCountryByIcao(metar.icao)?.name?.lowercase()?.contains(query) == true)
            }
        }

        // Сортировка
        filtered = when (sortOption) {
            SortOption.BY_CITY -> filtered.sortedWith(
                compareByDescending<MetarData> { it.icao in favorites }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
            SortOption.BY_ICAO -> filtered.sortedWith(
                compareByDescending<MetarData> { it.icao in favorites }
                    .thenBy { it.icao }
            )
            SortOption.BY_CATEGORY -> filtered.sortedWith(
                compareByDescending<MetarData> { it.icao in favorites }
                    .thenBy { it.flightCategory.ordinal }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
            SortOption.BY_VISIBILITY -> filtered.sortedWith(
                compareByDescending<MetarData> { it.icao in favorites }
                    .thenBy { it.visibilityM ?: Float.MAX_VALUE }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
            SortOption.BY_COUNTRY -> filtered.sortedWith(
                compareByDescending<MetarData> { it.icao in favorites }
                    .thenBy { AirportData.getCountryByIcao(it.icao)?.name ?: "" }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
        }

        _uiState.value = _uiState.value.copy(metarList = filtered)
    }

    /**
     * Автоматическое обновление каждые 3 минуты
     */
    private fun startAutoRefresh() {
        viewModelScope.launch {
            // Первоначальная загрузка
            loadMetar()

            // Периодическое обновление
            while (true) {
                delay(updateIntervalMs)
                loadMetar()
            }
        }
    }

    /**
     * Очищает ошибку
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

