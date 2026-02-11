package com.example.meteometar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteometar.data.MetarData
import com.example.meteometar.ui.components.MetarCard
import com.example.meteometar.ui.theme.DarkBackground
import com.example.meteometar.ui.theme.DarkSurface
import com.example.meteometar.viewmodel.MetarViewModel

/**
 * Экран избранных аэропортов
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: MetarViewModel,
    onMetarClick: (MetarData) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Фильтруем только избранные
    val favoriteMetars = uiState.metarList.filter { it.icao in uiState.favorites }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⭐ Избранное",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadMetar() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Статус
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Избранных: ${favoriteMetars.size}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Нажмите ⭐ чтобы убрать",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (favoriteMetars.isEmpty()) {
                    // Пустой список
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 64.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Нет избранных аэропортов",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нажмите на звёздочку рядом с аэропортом\nна главном экране, чтобы добавить его в избранное",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Список избранных
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = favoriteMetars,
                            key = { it.icao }
                        ) { metar ->
                            MetarCard(
                                metar = metar,
                                isFavorite = true,
                                onFavoriteClick = { viewModel.toggleFavorite(metar.icao) },
                                onClick = { onMetarClick(metar) }
                            )
                        }
                    }
                }
            }
        }
    }
}

