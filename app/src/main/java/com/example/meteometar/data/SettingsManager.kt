package com.example.meteometar.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Варианты сортировки
 */
enum class SortOption(val displayName: String) {
    BY_CITY("По городу (А-Я)"),
    BY_ICAO("По ICAO"),
    BY_CATEGORY("По категории полёта"),
    BY_VISIBILITY("По видимости"),
    BY_COUNTRY("По стране")
}

/**
 * Менеджер настроек приложения
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "meteo_settings", Context.MODE_PRIVATE
    )

    private val _selectedCountries = MutableStateFlow<Set<String>>(loadSelectedCountries())
    val selectedCountries: StateFlow<Set<String>> = _selectedCountries.asStateFlow()

    private val _sortOption = MutableStateFlow(loadSortOption())
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    /**
     * Загрузить выбранные страны
     */
    private fun loadSelectedCountries(): Set<String> {
        val saved = prefs.getStringSet(KEY_SELECTED_COUNTRIES, null)
        return saved ?: setOf("KZ") // По умолчанию только Казахстан
    }

    /**
     * Сохранить выбранные страны
     */
    fun setSelectedCountries(countries: Set<String>) {
        _selectedCountries.value = countries
        prefs.edit().putStringSet(KEY_SELECTED_COUNTRIES, countries).apply()
    }

    /**
     * Переключить страну
     */
    fun toggleCountry(countryCode: String) {
        val current = _selectedCountries.value.toMutableSet()
        if (countryCode in current) {
            if (current.size > 1) { // Минимум одна страна должна быть выбрана
                current.remove(countryCode)
            }
        } else {
            current.add(countryCode)
        }
        setSelectedCountries(current)
    }

    /**
     * Загрузить вариант сортировки
     */
    private fun loadSortOption(): SortOption {
        val saved = prefs.getString(KEY_SORT_OPTION, SortOption.BY_CITY.name)
        return try {
            SortOption.valueOf(saved ?: SortOption.BY_CITY.name)
        } catch (e: Exception) {
            SortOption.BY_CITY
        }
    }

    /**
     * Сохранить вариант сортировки
     */
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        prefs.edit().putString(KEY_SORT_OPTION, option.name).apply()
    }

    /**
     * Загрузить избранные аэропорты
     */
    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    /**
     * Переключить избранное
     */
    fun toggleFavorite(icao: String) {
        val current = _favorites.value.toMutableSet()
        if (icao in current) {
            current.remove(icao)
        } else {
            current.add(icao)
        }
        _favorites.value = current
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    /**
     * Проверить, является ли аэропорт избранным
     */
    fun isFavorite(icao: String): Boolean = icao in _favorites.value

    companion object {
        private const val KEY_SELECTED_COUNTRIES = "selected_countries"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_FAVORITES = "favorites"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

