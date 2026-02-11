package com.example.meteometar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteometar.data.AirportData
import com.example.meteometar.data.RunwayDatabase
import com.example.meteometar.data.TafData
import com.example.meteometar.ui.theme.*

/**
 * Карточка TAF данных
 */
@Composable
fun TafCard(
    taf: TafData,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val country = AirportData.getCountryByIcao(taf.icao)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Верхняя строка: Флаг, Город, ICAO, Избранное, TAF badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Флаг страны
                if (country != null) {
                    Text(
                        text = country.flag,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taf.cityName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = taf.icao,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        // Информация о ВПП
                        val runwayInfo = RunwayDatabase.getRunwayInfo(taf.icao)
                        if (runwayInfo.isNotEmpty()) {
                            Text(
                                text = " • ",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "ВПП: $runwayInfo",
                                fontSize = 12.sp,
                                color = Color(0xFF81C784),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Активная ВПП на основе ветра из TAF
                    val activeRwy = RunwayDatabase.getActiveRunway(taf.icao, taf.wind.directionDeg)
                    if (activeRwy != null) {
                        Text(
                            text = "✈ Рабочая: $activeRwy",
                            fontSize = 11.sp,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Кнопка избранного
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (isFavorite) Color(0xFFFFD700) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // TAF бейдж
                TafBadge()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Период действия
            val validPeriod = taf.getValidPeriodDisplay()
            if (validPeriod.isNotEmpty()) {
                Text(
                    text = "⏰ $validPeriod",
                    fontSize = 12.sp,
                    color = Color(0xFF81C784),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Данные в двух колонках
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая колонка
                Column(modifier = Modifier.weight(1f)) {
                    TafDataRow(label = "Ветер", value = taf.wind.toDisplayString())
                    TafDataRow(label = "Видимость", value = taf.getVisibilityString())
                }

                // Правая колонка
                Column(modifier = Modifier.weight(1f)) {
                    TafDataRow(label = "Облачность", value = taf.getCloudsDisplay())
                    TafDataRow(
                        label = "Изменения",
                        value = if (taf.changes.isNotEmpty()) "${taf.changes.size} прогноз(ов)" else "нет"
                    )
                }
            }

            // Погодные явления
            val weather = taf.getWeatherDisplay()
            if (weather.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Явления: $weather",
                    fontSize = 13.sp,
                    color = Color(0xFFFFB74D),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Время выпуска
            val issueTime = taf.getIssueTimeDisplay()
            if (issueTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Выпущен: $issueTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * TAF бейдж
 */
@Composable
fun TafBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF5C6BC0))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "TAF",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Строка данных TAF
 */
@Composable
fun TafDataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    if (value.isNotEmpty()) {
        Column(modifier = modifier.padding(vertical = 2.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
