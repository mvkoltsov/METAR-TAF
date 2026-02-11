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
 * Карточка METAR данных
 */
@Composable
fun MetarCard(
    metar: MetarData,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Верхняя строка: Флаг, Город, ICAO, Избранное, Категория
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
                        text = metar.cityName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = metar.icao,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
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

                // Бейдж категории
                CategoryBadge(category = metar.flightCategory)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Данные в двух колонках
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая колонка
                Column(modifier = Modifier.weight(1f)) {
                    DataRow(label = "Ветер", value = metar.wind.toDisplayString())
                    DataRow(label = "Видимость", value = metar.getVisibilityString())
                    DataRow(label = "Облачность", value = metar.getCloudsDisplay())
                }

                // Правая колонка
                Column(modifier = Modifier.weight(1f)) {
                    DataRow(
                        label = "Температура",
                        value = metar.tempC?.let { "${it.toInt()}°C" } ?: ""
                    )
                    DataRow(
                        label = "Точка росы",
                        value = metar.dewpointC?.let { "${it.toInt()}°C" } ?: ""
                    )
                    DataRow(
                        label = "QNH",
                        value = metar.qnhMmHg?.let { "$it мм.рт.ст" } ?: ""
                    )
                }
            }

            // Погодные явления
            val weather = metar.getWeatherDisplay()
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

            // Время наблюдения
            val time = metar.getTimeDisplay()
            if (time.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Время: $time",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
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

