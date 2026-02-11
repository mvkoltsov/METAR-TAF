package com.example.meteometar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.example.meteometar.data.TafData
import com.example.meteometar.ui.components.TafCard
import com.example.meteometar.ui.theme.DarkBackground
import com.example.meteometar.ui.theme.DarkSurface
import com.example.meteometar.viewmodel.TafViewModel

/**
 * Экран избранных TAF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafFavoritesScreen(
    viewModel: TafViewModel,
    onTafClick: (TafData) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Фильтруем только избранные
    val favoriteTafs = uiState.tafList.filter { it.icao in uiState.favorites }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⭐ Избранное TAF",
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
                actions = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFFFF6B6B)
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
            onRefresh = { viewModel.loadTaf() },
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
                        text = "Избранных: ${favoriteTafs.size}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Нажмите ⭐ чтобы убрать",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (favoriteTafs.isEmpty()) {
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
                                text = "Нет избранных прогнозов",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нажмите на звёздочку рядом с прогнозом\nчтобы добавить его в избранное",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = favoriteTafs,
                            key = { it.icao }
                        ) { taf ->
                            TafCard(
                                taf = taf,
                                isFavorite = true,
                                onFavoriteClick = { viewModel.toggleFavorite(taf.icao) },
                                onClick = { onTafClick(taf) }
                            )
                        }
                    }
                }
            }
        }
    }
}
