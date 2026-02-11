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
import com.example.meteometar.data.FlightCategory
import com.example.meteometar.data.MetarData
import com.example.meteometar.data.RunwayDatabase
import com.example.meteometar.ui.theme.*

/**
 * Возвращает цвет фона для категории полёта
 */
fun FlightCategory.getColor(): Color = when (this) {
    FlightCategory.VFR -> VfrColor
    FlightCategory.MVFR -> MvfrColor
    FlightCategory.IFR -> IfrColor
    FlightCategory.LIFR -> LifrColor
}

/**
 * Карточка METAR данных - КОМПАКТНАЯ версия
 */
@Composable
fun MetarCard(
    metar: MetarData,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onNotamClick: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val country = AirportData.getCountryByIcao(metar.icao)

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
            // Строка 1: Флаг, Город (ICAO), ВПП, Рабочая, ⭐, Категория, NOTAM
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
                            text = metar.cityName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " (${metar.icao})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    // ВПП + Рабочая в одну строку
                    val runwayInfo = RunwayDatabase.getRunwayInfo(metar.icao)
                    val activeRwy = RunwayDatabase.getActiveRunway(metar.icao, metar.wind.directionDeg)
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

                // Категория
                CompactCategoryBadge(category = metar.flightCategory)

                // NOTAM
                if (metar.notamCount > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    CompactNotamBadge(count = metar.notamCount, onClick = onNotamClick)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Строка 2: Ветер | Видимость | Облачность | Темп/Роса | QNH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ветер
                CompactDataItem(label = "💨", value = metar.wind.toDisplayString(), modifier = Modifier.weight(1f))
                // Видимость
                CompactDataItem(label = "👁", value = metar.getVisibilityString(), modifier = Modifier.weight(0.8f))
                // Облачность
                CompactDataItem(label = "☁", value = metar.getCloudsDisplay(), modifier = Modifier.weight(1f))
                // Температура/Роса
                val tempDew = "${metar.tempC?.toInt() ?: "-"}/${metar.dewpointC?.toInt() ?: "-"}°"
                CompactDataItem(label = "🌡", value = tempDew, modifier = Modifier.weight(0.7f))
                // QNH
                CompactDataItem(label = "📊", value = "${metar.qnhMmHg ?: "-"}", modifier = Modifier.weight(0.6f))
            }

            // Строка 3: Явления + Время (только если есть)
            val weather = metar.getWeatherDisplay()
            val time = metar.getTimeDisplay()
            if (weather.isNotEmpty() || time.isNotEmpty()) {
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
                    if (time.isNotEmpty()) {
                        Text(
                            text = time,
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
 * Компактный элемент данных
 */
@Composable
fun CompactDataItem(label: String, value: String, modifier: Modifier = Modifier) {
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

/**
 * Компактный бейдж категории
 */
@Composable
fun CompactCategoryBadge(category: FlightCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(category.getColor())
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Компактный индикатор NOTAM
 */
@Composable
fun CompactNotamBadge(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFF9800))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "⚠$count",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Бейдж категории полёта
 */
@Composable
fun CategoryBadge(category: FlightCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(category.getColor())
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = category.displayName,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Строка данных
 */
@Composable
fun DataRow(
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

/**
 * Компактная карточка для списка
 */
@Composable
fun MetarCompactCard(
    metar: MetarData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Город и ICAO
            Column(modifier = Modifier.weight(0.3f)) {
                Text(
                    text = metar.cityName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metar.icao,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Основные данные
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = metar.wind.toDisplayString(),
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = metar.getVisibilityString(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Категория
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(metar.flightCategory.getColor())
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = metar.flightCategory.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Индикатор NOTAM на карточке
 */
@Composable
fun NotamBadge(
    count: Int,
    hasImportant: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = if (hasImportant) Color(0xFFE53935) else Color(0xFFFF9800)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "⚠",
                fontSize = 10.sp,
                color = Color.White
            )
            Text(
                text = if (count > 1) "NOTAM($count)" else "NOTAM",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

