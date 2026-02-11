package com.example.meteometar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteometar.data.TafChange
import com.example.meteometar.data.TafData
import com.example.meteometar.ui.theme.DarkBackground
import com.example.meteometar.ui.theme.DarkCard
import com.example.meteometar.ui.theme.DarkSurface

/**
 * Экран детальной информации о TAF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafDetailScreen(
    taf: TafData,
    onBack: () -> Unit,
    onExit: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${taf.cityName} (${taf.icao})",
                        color = Color.White
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Заголовок TAF
            TafHeaderSection(taf)

            Spacer(modifier = Modifier.height(16.dp))

            // Основной прогноз
            TafMainForecastSection(taf)

            Spacer(modifier = Modifier.height(16.dp))

            // Погодные явления
            if (taf.weather.isNotEmpty()) {
                TafWeatherSection(taf)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Изменения прогноза
            if (taf.changes.isNotEmpty()) {
                TafChangesSection(taf.changes)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Raw TAF
            TafRawSection(taf.rawTaf)
        }
    }
}

@Composable
private fun TafHeaderSection(taf: TafData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TAF бейдж
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF5C6BC0))
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "TAF ПРОГНОЗ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Период действия
            val validPeriod = taf.getValidPeriodDisplay()
            if (validPeriod.isNotEmpty()) {
                Text(
                    text = "⏰ Период действия:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = validPeriod,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF81C784),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Время выпуска
            val issueTime = taf.getIssueTimeDisplay()
            if (issueTime.isNotEmpty()) {
                Text(
                    text = "Выпущен: $issueTime",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun TafMainForecastSection(taf: TafData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🌤️ Основной прогноз",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Данные
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TafDataItem(
                    label = "Ветер",
                    value = taf.wind.toDisplayString(),
                    modifier = Modifier.weight(1f)
                )
                TafDataItem(
                    label = "Видимость",
                    value = taf.getVisibilityString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Облачность
            if (taf.clouds.isNotEmpty()) {
                Text(
                    text = "Облачность",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                taf.clouds.forEach { cloud ->
                    Text(
                        text = "☁️ ${cloud.toDisplayString()}",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TafDataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.ifEmpty { "-" },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun TafWeatherSection(taf: TafData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "⚠️ Погодные явления",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            taf.weather.forEachIndexed { index, code ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB74D))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = code,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = taf.getWeatherDisplay(),
                fontSize = 15.sp,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun TafChangesSection(changes: List<TafChange>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🔄 Ожидаемые изменения",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            changes.forEachIndexed { index, change ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TafChangeItem(change)
            }
        }
    }
}

@Composable
private fun TafChangeItem(change: TafChange) {
    Column {
        // Индикатор изменения
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF5C6BC0))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = change.getIndicatorDisplayName(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ветер
        change.wind?.let {
            Text(
                text = "🌬️ Ветер: ${it.toDisplayString()}",
                fontSize = 14.sp,
                color = Color.White
            )
        }

        // Видимость
        change.visibilityM?.let { vis ->
            val visText = when {
                vis >= 10000 -> "≥10 км"
                vis >= 1000 -> "${(vis / 1000).toInt()} км"
                else -> "${vis.toInt()} м"
            }
            Text(
                text = "👁️ Видимость: $visText",
                fontSize = 14.sp,
                color = Color.White
            )
        }

        // Погода
        if (change.weather.isNotEmpty()) {
            Text(
                text = "⚠️ Явления: ${change.weather.joinToString(", ")}",
                fontSize = 14.sp,
                color = Color(0xFFFFB74D)
            )
        }

        // Облачность
        if (change.clouds.isNotEmpty()) {
            change.clouds.forEach { cloud ->
                Text(
                    text = "☁️ ${cloud.toDisplayString()}",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TafRawSection(raw: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📄 Исходная сводка TAF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = raw,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF81C784),
                textAlign = TextAlign.Start,
                lineHeight = 20.sp
            )
        }
    }
}
