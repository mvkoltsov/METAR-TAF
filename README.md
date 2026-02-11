# METAR TAF - Авиационная погода для Android

🛫 Android приложение для получения и отображения авиационных метеосводок METAR и прогнозов TAF для аэропортов Казахстана и соседних стран.

## 📱 Возможности

### METAR (Текущая погода)
- ✅ Актуальные метеосводки METAR для 300+ аэропортов
- 🌍 Поддержка 16+ стран (Казахстан, Россия, Узбекистан, Кыргызстан, Туркменистан, Китай, Монголия и др.)
- 🔍 Поиск по ИКАО коду, городу, погодным явлениям
- ⭐ Избранные аэропорты
- 📊 5 вариантов сортировки
- 🔄 Pull-to-Refresh
- ⏱️ Автообновление каждые 3 минуты

### TAF (Прогноз погоды)
- 📅 Прогнозы TAF на 24-30 часов
- 🔮 Детальная информация об ожидаемых изменениях
- 🌤️ Декодирование прогнозов на русском языке
- ⚠️ Погодные явления и изменения
- ⏱️ Автообновление каждые 5 минут

### Интерфейс
- 🌙 Тёмная тема
- 📱 Полноэкранный режим
- 🔀 Переключение METAR ↔ TAF
- 🎨 Material3 дизайн
- 🇷🇺 Полностью на русском языке

## 🛠️ Технологии

- **Kotlin** - язык программирования
- **Jetpack Compose** - современный UI фреймворк
- **MVVM** - архитектурный паттерн
- **Coroutines & Flow** - асинхронность
- **Aviation Weather API** - источник данных METAR/TAF
- **Material3** - дизайн система

## 📦 Структура проекта

```
app/src/main/java/com/example/meteometar/
├── data/
│   ├── AirportData.kt        # База аэропортов
│   ├── MetarData.kt           # Модели METAR
│   ├── TafData.kt             # Модели TAF
│   ├── MetarParser.kt         # Парсер METAR
│   ├── TafParser.kt           # Парсер TAF
│   └── SettingsManager.kt     # Настройки приложения
├── network/
│   ├── MetarRepository.kt     # Загрузка METAR
│   └── TafRepository.kt       # Загрузка TAF
├── viewmodel/
│   ├── MetarViewModel.kt      # ViewModel для METAR
│   └── TafViewModel.kt        # ViewModel для TAF
├── ui/
│   ├── screens/
│   │   ├── MetarListScreen.kt
│   │   ├── TafListScreen.kt
│   │   ├── MetarDetailScreen.kt
│   │   ├── TafDetailScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   └── TafFavoritesScreen.kt
│   ├── components/
│   │   ├── MetarCard.kt
│   │   └── TafCard.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

## 🚀 Сборка и установка

### Требования
- Android Studio Koala или новее
- JDK 17+
- Android SDK с минимальной версией API 26 (Android 8.0)

### Сборка APK

#### В Android Studio:
1. Откройте проект в Android Studio
2. Build → Build Bundle(s) / APK(s) → Build APK(s)
3. APK будет в `app/build/outputs/apk/debug/`

#### Из командной строки:
```bash
# Windows
gradlew assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

## 🌐 Поддерживаемые страны

- 🇰🇿 Казахстан (22 аэропорта)
- 🇷🇺 Россия (40+ аэропортов)
- 🇺🇿 Узбекистан (12 аэропортов)
- 🇰🇬 Кыргызстан (7 аэропортов)
- 🇹🇯 Таджикистан (5 аэропортов)
- 🇹🇲 Туркменистан (6 аэропортов)
- 🇨🇳 Китай (20+ аэропортов)
- 🇲🇳 Монголия (11 аэропортов)
- 🇦🇿 Азербайджан (7 аэропортов)
- 🇬🇪 Грузия (4 аэропорта)
- 🇦🇲 Армения (3 аэропорта)
- 🇮🇷 Иран (17 аэропортов)
- 🇹🇷 Турция (15 аэропортов)
- 🇦🇫 Афганистан (6 аэропортов)
- 🇵🇰 Пакистан (8 аэропортов)
- 🇮🇳 Индия (12 аэропортов)

## 📸 Скриншоты

(Здесь можно добавить скриншоты приложения)

## 📄 Лицензия

MIT License

## 👤 Автор

mvkoltsov

## 🙏 Благодарности

- [Aviation Weather Center (NOAA)](https://aviationweather.gov) - за API данных METAR/TAF
- Jetpack Compose команда за отличный UI фреймворк

---

Made with ❤️ for aviation enthusiasts
