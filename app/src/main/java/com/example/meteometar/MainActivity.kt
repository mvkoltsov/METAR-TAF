package com.example.meteometar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meteometar.data.MetarData
import com.example.meteometar.data.TafData
import com.example.meteometar.ui.screens.*
import com.example.meteometar.ui.theme.DarkSurface
import com.example.meteometar.ui.theme.METEOMETARTheme
import com.example.meteometar.viewmodel.MetarViewModel
import com.example.meteometar.viewmodel.TafViewModel

/**
 * Основные вкладки приложения
 */
enum class MainTab {
    METAR, TAF
}

/**
 * Экраны приложения
 */
sealed class Screen {
    data object MetarList : Screen()
    data object MetarFavorites : Screen()
    data class MetarDetail(val metar: MetarData) : Screen()

    data object TafList : Screen()
    data object TafFavorites : Screen()
    data class TafDetail(val taf: TafData) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем полноэкранный режим
        enableEdgeToEdge()

        // Настраиваем иммерсивный полноэкранный режим
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Скрываем системные панели
            hide(WindowInsetsCompat.Type.systemBars())
            // Поведение при свайпе - показываем панели временно
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Экран не гаснет пока приложение открыто
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            METEOMETARTheme(dynamicColor = false) {
                MeteoApp(
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun MeteoApp(onExit: () -> Unit) {
    val metarViewModel: MetarViewModel = viewModel()
    val tafViewModel: TafViewModel = viewModel()

    // Текущая вкладка
    var currentTab by remember { mutableStateOf(MainTab.METAR) }

    // Навигация через состояние
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MetarList) }

    // Диалог подтверждения выхода
    var showExitDialog by remember { mutableStateOf(false) }

    // При смене вкладки переходим на соответствующий список
    LaunchedEffect(currentTab) {
        currentScreen = when (currentTab) {
            MainTab.METAR -> Screen.MetarList
            MainTab.TAF -> Screen.TafList
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Кнопка закрытия и переключатель вкладок (показываем только на главных экранах)
        val showTabBar = currentScreen is Screen.MetarList || currentScreen is Screen.TafList
        if (showTabBar) {
            Surface(
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                // Всё в одну строку: переключатели METAR/TAF + поиск + кнопка X
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Переключатели METAR/TAF
                    Row(
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CompactTabButton(
                            text = "METAR",
                            isSelected = currentTab == MainTab.METAR,
                            selectedColor = Color(0xFF3D7AD9),
                            onClick = { currentTab = MainTab.METAR }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        CompactTabButton(
                            text = "TAF",
                            isSelected = currentTab == MainTab.TAF,
                            selectedColor = Color(0xFF5C6BC0),
                            onClick = { currentTab = MainTab.TAF }
                        )
                    }

                    // Поиск (занимает оставшееся место)
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        when (val screen = currentScreen) {
                            is Screen.MetarList -> {
                                CompactSearchField(
                                    searchQuery = metarViewModel.uiState.collectAsState().value.searchQuery,
                                    onSearchChange = metarViewModel::updateSearchQuery
                                )
                            }
                            is Screen.TafList -> {
                                CompactSearchField(
                                    searchQuery = tafViewModel.uiState.collectAsState().value.searchQuery,
                                    onSearchChange = tafViewModel::updateSearchQuery
                                )
                            }
                            else -> {}
                        }
                    }

                    // Кнопка закрытия X
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                    }
                }
            }
        }

        // Контент
        Box(modifier = Modifier.weight(1f)) {
            when (val screen = currentScreen) {
                // METAR экраны
                is Screen.MetarList -> {
                    MetarListScreen(
                        viewModel = metarViewModel,
                        onMetarClick = { metar -> currentScreen = Screen.MetarDetail(metar) },
                        onFavoritesClick = { currentScreen = Screen.MetarFavorites },
                        onExit = { showExitDialog = true }
                    )
                }
                is Screen.MetarFavorites -> {
                    FavoritesScreen(
                        viewModel = metarViewModel,
                        onMetarClick = { metar -> currentScreen = Screen.MetarDetail(metar) },
                        onBack = { currentScreen = Screen.MetarList },
                        onExit = { showExitDialog = true }
                    )
                }
                is Screen.MetarDetail -> {
                    MetarDetailScreen(
                        metar = screen.metar,
                        onBack = { currentScreen = Screen.MetarList },
                        onExit = { showExitDialog = true }
                    )
                }

                // TAF экраны
                is Screen.TafList -> {
                    TafListScreen(
                        viewModel = tafViewModel,
                        onTafClick = { taf -> currentScreen = Screen.TafDetail(taf) },
                        onFavoritesClick = { currentScreen = Screen.TafFavorites },
                        onExit = { showExitDialog = true }
                    )
                }
                is Screen.TafFavorites -> {
                    TafFavoritesScreen(
                        viewModel = tafViewModel,
                        onTafClick = { taf -> currentScreen = Screen.TafDetail(taf) },
                        onBack = { currentScreen = Screen.TafList },
                        onExit = { showExitDialog = true }
                    )
                }
                is Screen.TafDetail -> {
                    TafDetailScreen(
                        taf = screen.taf,
                        onBack = { currentScreen = Screen.TafList },
                        onExit = { showExitDialog = true }
                    )
                }
            }
        }
    }

    // Диалог подтверждения выхода
    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = onExit,
            onDismiss = { showExitDialog = false }
        )
    }
}

/**
 * Компактная кнопка переключателя
 */
@Composable
fun CompactTabButton(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) selectedColor else Color.Transparent
    val textColor = if (isSelected) Color.White else Color.Gray
    val borderColor = if (isSelected) selectedColor else Color.Gray

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .width(80.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 0.dp else 1.dp,
            color = borderColor
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Компактное поле поиска
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSearchField(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        placeholder = {
            Text(
                text = "Поиск...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Поиск",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Очистить поиск",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF3D7AD9),
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            color = Color.White
        )
    )
}

/**
 * Диалог подтверждения выхода из приложения
 */
@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "⚠️",
                fontSize = 48.sp
            )
        },
        title = {
            Text(
                text = "Закрыть приложение?",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Вы уверены, что хотите выйти из приложения?",
                color = Color.White,
                fontSize = 15.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B)
                )
            ) {
                Text(
                    text = "Да, закрыть",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Отмена")
            }
        },
        containerColor = DarkSurface
    )
}

