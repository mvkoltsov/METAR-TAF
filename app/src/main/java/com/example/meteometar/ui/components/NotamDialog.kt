package com.example.meteometar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.meteometar.data.NotamData
import com.example.meteometar.data.NotamImportance
import com.example.meteometar.data.NotamType
import com.example.meteometar.ui.theme.DarkCard
import com.example.meteometar.ui.theme.DarkSurface

/**
 * Диалог со списком NOTAM
 */
@Composable
fun NotamDialog(
    icao: String,
    cityName: String,
    notamList: List<NotamData>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOTAM - $icao",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = cityName,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Text(
                        text = "${notamList.size} шт.",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Список NOTAM
                if (notamList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет активных NOTAM",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notamList) { notam ->
                            NotamItem(notam = notam)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3D7AD9)
                    )
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

/**
 * Один элемент NOTAM в списке
 */
@Composable
fun NotamItem(notam: NotamData) {
    val importanceColor = when (notam.decoded?.importance) {
        NotamImportance.CRITICAL -> Color(0xFFE53935)
        NotamImportance.WARNING -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Заголовок NOTAM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ID и тип
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = notam.id,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Тип NOTAM
                    val typeText = when (notam.type) {
                        NotamType.NEW -> "НОВЫЙ"
                        NotamType.REPLACE -> "ЗАМЕНА"
                        NotamType.CANCEL -> "ОТМЕНА"
                    }
                    val typeColor = when (notam.type) {
                        NotamType.NEW -> Color(0xFF4CAF50)
                        NotamType.REPLACE -> Color(0xFFFF9800)
                        NotamType.CANCEL -> Color(0xFFE53935)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }

                // Индикатор важности
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(importanceColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Расшифрованное описание
            notam.decoded?.let { decoded ->
                // Предмет и состояние
                Text(
                    text = "${decoded.subject} - ${decoded.condition}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = importanceColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Полное описание
                Text(
                    text = decoded.description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Период действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Начало:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = notam.effectiveFrom.ifEmpty { "—" },
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Окончание:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = if (notam.isPermanent) "Постоянно" else notam.effectiveTo.ifEmpty { "—" },
                        fontSize = 12.sp,
                        color = if (notam.isPermanent) Color(0xFFE53935) else Color.White
                    )
                }
            }

            // Исходный текст (свёрнутый)
            var showRaw by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showRaw = !showRaw },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (showRaw) "▲ Скрыть оригинал" else "▼ Показать оригинал",
                    fontSize = 12.sp,
                    color = Color(0xFF64B5F6)
                )
            }

            if (showRaw) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = notam.rawText,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
