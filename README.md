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

### NOTAM (Уведомления для лётчиков)
- 📋 Получение действующих NOTAM для аэродромов Казахстана
- 🌐 Парсинг с официальных источников Казаэронавигации
- 🔤 Декодирование NOTAM в человеко-читаемый формат
- 🚨 Классификация критичности (КРИТИЧНО/ПРЕДУПРЕЖДЕНИЕ/ИНФОРМАЦИЯ)
- 🎨 92 Q-кода и 167 авиационных аббревиатур на русском языке
- 📝 Python модули для сбора и декодирования

### Интерфейс
- 🌙 Тёмная тема
- 📱 Полноэкранный режим
- 🔀 Переключение METAR ↔ TAF
- 🎨 Material3 дизайн
- 🇷🇺 Полностью на русском языке

## 🛠️ Технологии

### Android приложение
- **Kotlin** - язык программирования
- **Jetpack Compose** - современный UI фреймворк
- **MVVM** - архитектурный паттерн
- **Coroutines & Flow** - асинхронность
- **Aviation Weather API** - источник данных METAR/TAF
- **Material3** - дизайн система

### Python модули (NOTAM)
- **Python 3** - язык программирования
- **requests** - HTTP запросы
- **BeautifulSoup4** - парсинг HTML
- **lxml** - обработка XML/HTML

## 📦 Структура проекта

### Android приложение
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

### Python модули для NOTAM
```
├── collect_notam.py      # Сбор NOTAM из источников Казаэронавигации
├── notam_decoder.py       # Декодер NOTAM в человеко-читаемый формат
├── collect_taf.py         # Сбор TAF из международных источников
├── taf_decoder.py         # Декодер TAF в человеко-читаемый формат
└── requirements.txt       # Зависимости Python
```

## 🐍 Использование Python модулей

### Установка зависимостей
```bash
pip install -r requirements.txt
```

### Сбор NOTAM
```bash
# Все NOTAM по Казахстану
python3 collect_notam.py

# NOTAM для конкретного аэродрома
python3 collect_notam.py --icao UAAA

# Сохранение в JSON файл
python3 collect_notam.py --output notam_data.json

# Список аэродромов
python3 collect_notam.py --list-airports
```

### Декодирование NOTAM
```bash
# Декодирование из файла
python3 notam_decoder.py --file notam_data.json

# Декодирование для конкретного аэродрома
python3 notam_decoder.py --file notam_data.json --icao UAAA

# Вывод в JSON формате
python3 notam_decoder.py --file notam_data.json --json

# Сохранение результата
python3 notam_decoder.py --file notam_data.json --output decoded.txt
```

### Сбор TAF
```bash
# Все TAF
python3 collect_taf.py

# Только Казахстан
python3 collect_taf.py --country KZ

# Сохранение в файл
python3 collect_taf.py --output taf_data.json
```

### Декодирование TAF
```bash
# Декодирование из файла
python3 taf_decoder.py --file taf_data.json

# Декодирование текста
python3 taf_decoder.py "TAF UAAA 101100Z 1012/1112 32015G25KT 9999 FEW040"
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
