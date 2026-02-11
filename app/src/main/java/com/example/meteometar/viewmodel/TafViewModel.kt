package com.example.meteometar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meteometar.data.AirportData
import com.example.meteometar.data.Country
import com.example.meteometar.data.SettingsManager
import com.example.meteometar.data.SortOption
import com.example.meteometar.data.TafData
import com.example.meteometar.network.TafRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Состояние UI для TAF
 */
data class TafUiState(
    val tafList: List<TafData> = emptyList(),
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
 * ViewModel для управления TAF данными
 */
class TafViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager.getInstance(application)

    private val _uiState = MutableStateFlow(TafUiState())
    val uiState: StateFlow<TafUiState> = _uiState.asStateFlow()

    // Все загруженные данные
    private var allTafData: Map<String, TafData> = emptyMap()

    // Интервал обновления в миллисекундах (5 минут для TAF)
    private val updateIntervalMs = 5 * 60 * 1000L

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
                if (allTafData.isEmpty() ||
                    _uiState.value.selectedCountries != countries) {
                    loadTaf()
                } else {
                    applyFilter()
                }
            }
        }

        startAutoRefresh()
    }

    /**
     * Загружает TAF данные
     */
    fun loadTaf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val countryIcaoList = AirportData.getIcaoListForCountries(_uiState.value.selectedCountries)
            val favorites = _uiState.value.favorites
            val allIcaoList = (countryIcaoList + favorites).distinct()

            TafRepository.fetchTafForAirports(allIcaoList)
                .onSuccess { data ->
                    allTafData = data
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
            allTafData.values.toList()
        } else {
            allTafData.values.filter { taf ->
                taf.icao.lowercase().contains(query) ||
                taf.cityName.lowercase().contains(query) ||
                taf.getWeatherDisplay().lowercase().contains(query) ||
                taf.getCloudsDisplay().lowercase().contains(query) ||
                (AirportData.getCountryByIcao(taf.icao)?.name?.lowercase()?.contains(query) == true)
            }
        }

        // Сортировка
        filtered = when (sortOption) {
            SortOption.BY_CITY -> filtered.sortedWith(
                compareByDescending<TafData> { it.icao in favorites }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
            SortOption.BY_ICAO -> filtered.sortedWith(
                compareByDescending<TafData> { it.icao in favorites }
                    .thenBy { it.icao }
            )
            SortOption.BY_CATEGORY -> filtered.sortedWith(
                compareByDescending<TafData> { it.icao in favorites }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
            SortOption.BY_VISIBILITY -> filtered.sortedWith(
                compareByDescending<TafData> { it.icao in favorites }
                    .thenBy { it.visibilityM ?: Float.MAX_VALUE }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
            SortOption.BY_COUNTRY -> filtered.sortedWith(
                compareByDescending<TafData> { it.icao in favorites }
                    .thenBy { AirportData.getCountryByIcao(it.icao)?.name ?: "" }
                    .thenBy { AirportData.getCityName(it.icao) }
            )
        }

        _uiState.value = _uiState.value.copy(tafList = filtered)
    }

    /**
     * Автоматическое обновление
     */
    private fun startAutoRefresh() {
        viewModelScope.launch {
            loadTaf()
            while (true) {
                delay(updateIntervalMs)
                loadTaf()
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
