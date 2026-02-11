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
 * Карточка TAF данных - КОМПАКТНАЯ версия
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Строка 1: Флаг, Город (ICAO), ВПП, Рабочая, ⭐, TAF badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Флаг
                if (country != null) {
                    Text(text = country.flag, fontSize = 16.sp)
                }

                // Город (ICAO) - основная информация
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = taf.cityName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " (${taf.icao})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    // ВПП + Рабочая в одну строку
                    val runwayInfo = RunwayDatabase.getRunwayInfo(taf.icao)
                    val activeRwy = RunwayDatabase.getActiveRunway(taf.icao, taf.wind.directionDeg)
                    if (runwayInfo.isNotEmpty() || activeRwy != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (runwayInfo.isNotEmpty()) {
                                Text(text = runwayInfo, fontSize = 10.sp, color = Color(0xFF81C784))
                            }
                            if (activeRwy != null) {
                                Text(text = if (runwayInfo.isNotEmpty()) " → $activeRwy" else "→ $activeRwy",
                                     fontSize = 10.sp, color = Color(0xFF64B5F6), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Избранное
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // TAF badge (компактный)
                CompactTafBadge()
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Строка 2: Период | Ветер | Видимость | Облачность | Изменения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Период действия
                val validPeriod = taf.getValidPeriodDisplay()
                if (validPeriod.isNotEmpty()) {
                    Text(
                        text = validPeriod,
                        fontSize = 10.sp,
                        color = Color(0xFF81C784),
                        maxLines = 1,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                // Ветер
                TafCompactDataItem(label = "💨", value = taf.wind.toDisplayString(), modifier = Modifier.weight(1f))
                // Видимость
                TafCompactDataItem(label = "👁", value = taf.getVisibilityString(), modifier = Modifier.weight(0.8f))
                // Облачность
                TafCompactDataItem(label = "☁", value = taf.getCloudsDisplay(), modifier = Modifier.weight(1f))
                // Изменения
                val changesText = if (taf.changes.isNotEmpty()) "${taf.changes.size}" else "-"
                TafCompactDataItem(label = "📋", value = changesText, modifier = Modifier.weight(0.5f))
            }

            // Строка 3: Явления + Время (только если есть)
            val weather = taf.getWeatherDisplay()
            val issueTime = taf.getIssueTimeDisplay()
            if (weather.isNotEmpty() || issueTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (weather.isNotEmpty()) {
                        Text(
                            text = weather,
                            fontSize = 11.sp,
                            color = Color(0xFFFFB74D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (issueTime.isNotEmpty()) {
                        Text(
                            text = issueTime,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Компактный TAF бейдж
 */
@Composable
fun CompactTafBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF5C6BC0))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "TAF",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Компактный элемент данных TAF
 */
@Composable
fun TafCompactDataItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp)
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
