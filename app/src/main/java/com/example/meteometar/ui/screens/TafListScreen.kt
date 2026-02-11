package com.example.meteometar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteometar.data.Country
import com.example.meteometar.data.SortOption
import com.example.meteometar.data.TafData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.meteometar.ui.components.TafCard
import com.example.meteometar.ui.theme.DarkBackground
import com.example.meteometar.ui.theme.DarkSurface
import com.example.meteometar.viewmodel.TafViewModel

/**
 * Экран со списком TAF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafListScreen(
    viewModel: TafViewModel,
    onTafClick: (TafData) -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Выбор страны
                    TafCountryDropdown(
                        countries = uiState.countries,
                        selectedCountries = uiState.selectedCountries,
                        onCountryToggle = viewModel::toggleCountry
                    )

                    // Статус: количество и время
                    TafCompactStatusInfo(
                        count = uiState.tafList.size,
                        lastUpdate = uiState.lastUpdate,
                        isLoading = uiState.isLoading
                    )

                    // Кнопки
                    TafTopBar(
                        searchQuery = uiState.searchQuery,
                        onSearchChange = viewModel::updateSearchQuery,
                        onRefresh = viewModel::loadTaf,
                        isLoading = uiState.isLoading,
                        sortOption = uiState.sortOption,
                        onSortClick = { showSortMenu = true },
                        favoritesCount = uiState.favorites.size,
                        onFavoritesClick = onFavoritesClick,
                        onExit = onExit
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadTaf() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Список TAF
                if (uiState.isLoading && uiState.tafList.isEmpty()) {
                    TafLoadingContent()
                } else if (uiState.tafList.isEmpty()) {
                    TafEmptyContent(
                        message = if (uiState.searchQuery.isNotEmpty()) {
                            "Ничего не найдено по запросу \"${uiState.searchQuery}\""
                        } else {
                            "Нет данных. Выберите страны и нажмите обновить."
                        }
                    )
                } else {
                    TafList(
                        tafList = uiState.tafList,
                        favorites = uiState.favorites,
                        onTafClick = onTafClick,
                        onFavoriteClick = viewModel::toggleFavorite
                    )
                }
            }
        }
    }

    // Меню выбора сортировки
    if (showSortMenu) {
        TafSortMenuDialog(
            currentOption = uiState.sortOption,
            onOptionSelected = {
                viewModel.setSortOption(it)
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }

    // Показываем ошибку
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
}

/**
 * Компактный выбор страны для TAF
 */
@Composable
fun TafCountryDropdown(
    countries: List<Country>,
    selectedCountries: Set<String>,
    onCountryToggle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedText = if (selectedCountries.isEmpty()) {
        "Страны"
    } else {
        val count = selectedCountries.size
        if (count == 1) {
            val country = countries.find { it.code in selectedCountries }
            "${country?.flag ?: ""} ${country?.name ?: "Страны"}"
        } else {
            val country = countries.find { it.code in selectedCountries }
            "${country?.flag ?: ""} ${country?.name ?: ""} +${count - 1}"
        }
    }

    Box(
        modifier = Modifier
            .width(130.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.Gray
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedText,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(DarkSurface)
        ) {
            countries.forEach { country ->
                val isSelected = country.code in selectedCountries
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${country.flag} ${country.name}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color(0xFF5C6BC0),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    onClick = {
                        onCountryToggle(country.code)
                    }
                )
            }
        }
    }
}

/**
 * Диалог сортировки
 */
@Composable
fun TafSortMenuDialog(
    currentOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Сортировка",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == currentOption,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF5C6BC0)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.displayName,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = Color(0xFF5C6BC0))
            }
        },
        containerColor = DarkSurface
    )
}

/**
 * Компактная панель только с кнопками TAF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    sortOption: SortOption,
    onSortClick: () -> Unit,
    favoritesCount: Int,
    onFavoritesClick: () -> Unit,
    onExit: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Кнопка избранного
        BadgedBox(
            badge = {
                if (favoritesCount > 0) {
                    Badge(
                        containerColor = Color(0xFFFFD700)
                    ) {
                        Text(
                            text = favoritesCount.toString(),
                            fontSize = 9.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        ) {
            IconButton(
                onClick = onFavoritesClick,
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "⭐",
                    fontSize = 16.sp
                )
            }
        }

        // Кнопка сортировки
        IconButton(
            onClick = onSortClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Сортировка",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Кнопка обновления
        IconButton(
            onClick = onRefresh,
            enabled = !isLoading,
            modifier = Modifier.size(36.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Компактная информация о статусе TAF
 */
@Composable
fun TafCompactStatusInfo(
    count: Int,
    lastUpdate: Long,
    isLoading: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Аэродромов: $count",
            fontSize = 11.sp,
            color = Color.Gray
        )
        if (isLoading) {
            Text(
                text = "Обновление...",
                fontSize = 10.sp,
                color = Color(0xFF5C6BC0)
            )
        } else if (lastUpdate > 0) {
            Text(
                text = "Обн: ${timeFormat.format(Date(lastUpdate))}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * Статус бар (оставлен для совместимости)
 */
@Composable
fun TafStatusBar(
    count: Int,
    lastUpdate: Long,
    isLoading: Boolean,
    error: String?
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Прогнозов: $count",
            fontSize = 13.sp,
            color = Color.Gray
        )

        if (error != null) {
            Text(
                text = "⚠️ Ошибка",
                fontSize = 13.sp,
                color = Color(0xFFFF6B6B)
            )
        } else if (isLoading) {
            Text(
                text = "Загрузка...",
                fontSize = 13.sp,
                color = Color(0xFF5C6BC0)
            )
        } else if (lastUpdate > 0) {
            Text(
                text = "Обновлено: ${timeFormat.format(Date(lastUpdate))}",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * Список TAF
 */
@Composable
fun TafList(
    tafList: List<TafData>,
    favorites: Set<String>,
    onTafClick: (TafData) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = tafList,
            key = { it.icao }
        ) { taf ->
            TafCard(
                taf = taf,
                isFavorite = taf.icao in favorites,
                onFavoriteClick = { onFavoriteClick(taf.icao) },
                onClick = { onTafClick(taf) }
            )
        }
    }
}

/**
 * Индикатор загрузки
 */
@Composable
fun TafLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF5C6BC0),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Загрузка данных TAF...",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * Пустой контент
 */
@Composable
fun TafEmptyContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}
