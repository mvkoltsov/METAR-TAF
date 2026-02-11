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
import com.example.meteometar.data.MetarData
import com.example.meteometar.ui.components.getColor
import com.example.meteometar.ui.theme.DarkBackground
import com.example.meteometar.ui.theme.DarkCard
import com.example.meteometar.ui.theme.DarkSurface

/**
 * Экран детальной информации о METAR
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetarDetailScreen(
    metar: MetarData,
    onBack: () -> Unit,
    onExit: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${metar.cityName} (${metar.icao})",
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
            // Категория полёта
            CategorySection(metar)

            Spacer(modifier = Modifier.height(16.dp))

            // Основные данные
            DataSection(metar)

            Spacer(modifier = Modifier.height(16.dp))

            // Погодные явления
            if (metar.weather.isNotEmpty()) {
                WeatherSection(metar)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Raw METAR
            metar.rawMetar?.let { raw ->
                RawMetarSection(raw)
            }
        }
    }
}

@Composable
private fun CategorySection(metar: MetarData) {
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
            Text(
                text = "Категория полёта",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(metar.flightCategory.getColor())
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = metar.flightCategory.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = metar.flightCategory.displayName,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val time = metar.getTimeDisplay()
            if (time.isNotEmpty()) {
                Text(
                    text = "Наблюдение: $time",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DataSection(metar: MetarData) {
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
                text = "Метеоданные",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Сетка данных 2x3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DataItem(
                    label = "Ветер",
                    value = metar.wind.toDisplayString(),
                    modifier = Modifier.weight(1f)
                )
                DataItem(
                    label = "Видимость",
                    value = metar.getVisibilityString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DataItem(
                    label = "Температура",
                    value = metar.tempC?.let { "${it.toInt()}°C" } ?: "-",
                    modifier = Modifier.weight(1f)
                )
                DataItem(
                    label = "Точка росы",
                    value = metar.dewpointC?.let { "${it.toInt()}°C" } ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DataItem(
                    label = "QNH (мм.рт.ст)",
                    value = metar.qnhMmHg?.toString() ?: "-",
                    modifier = Modifier.weight(1f)
                )
                DataItem(
                    label = "QNH (hPa)",
                    value = metar.qnhHpa?.toInt()?.toString() ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Облачность
            if (metar.clouds.isNotEmpty()) {
                Text(
                    text = "Облачность",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                metar.clouds.forEach { cloud ->
                    Text(
                        text = cloud.toDisplayString(),
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DataItem(
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
private fun WeatherSection(metar: MetarData) {
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
                text = "Погодные явления",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Показываем коды и их расшифровку
            metar.weather.forEachIndexed { index, code ->
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

                    Spacer(modifier = Modifier.width(12.dp))

                    // Расшифровка уже сделана в MetarData
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = metar.getWeatherDisplay(),
                fontSize = 15.sp,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun RawMetarSection(raw: String) {
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
                text = "Исходная сводка METAR",
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

