# METEO METAR - Android приложение

Приложение для получения и отображения метеосводок METAR казахстанских аэропортов.

## Требования для сборки

- **Android Studio** Koala или новее
- **JDK 17+** (можно использовать встроенный JDK из Android Studio)
- **Android SDK** с минимальной версией API 26 (Android 8.0)

## Установка и настройка

1. **Распакуйте архив** в любую папку на компьютере

2. **Откройте Android Studio** и выберите "Open an Existing Project"

3. **Выберите папку** с распакованным проектом

4. **Дождитесь синхронизации** Gradle (автоматически загрузит зависимости)

5. **Настройте JDK** (если потребуется):
   - File → Project Structure → SDK Location
   - Укажите путь к JDK 17+ или используйте встроенный JDK из Android Studio

## Сборка APK

### В Android Studio:
- Build → Build Bundle(s) / APK(s) → Build APK(s)

### Из командной строки:
```bash
# Для Windows:
gradlew.bat assembleDebug

# Для Linux/Mac:
./gradlew assembleDebug
```

APK будет создан в: `app/build/outputs/apk/debug/app-debug.apk`

## Структура проекта

```
app/src/main/java/com/example/meteometar/
├── MainActivity.kt              # Главная активность
├── data/
│   ├── AirportData.kt          # Данные казахстанских аэропортов
│   ├── MetarData.kt            # Модели данных METAR
│   └── MetarParser.kt          # Парсер METAR строк
├── network/
│   └── MetarRepository.kt      # HTTP клиент для получения данных
├── viewmodel/
│   └── MetarViewModel.kt       # ViewModel с логикой приложения
└── ui/
    ├── components/
    │   └── MetarCard.kt        # UI компоненты
    ├── screens/
    │   ├── MetarListScreen.kt  # Главный экран
    │   └── MetarDetailScreen.kt # Детальный просмотр
    └── theme/                   # Тема приложения
```

## Особенности приложения

- **22 казахстанских аэропорта**: от Алматы до Жезказгана
- **Автообновление** каждые 3 минуты
- **Парсинг METAR**: ветер, видимость, облачность, температура, QNH
- **Категории полёта**: VFR/MVFR/IFR/LIFR с цветовой индикацией
- **Поиск** по ICAO, городу, погодным явлениям
- **Тёмная тема** с удобным интерфейсом
- **Перевод на русский** всех погодных явлений

## Источники данных

1. **Aviation Weather Center API** (основной)
2. **TGFTP NOAA** (резервный)

## Разработано на основе

Python скрипта `meteo.py` с упрощением функциональности:
- Убраны мультиязычность и ручной ввод
- Оставлены только казахстанские аэропорты
- Адаптирован под мобильную платформу

## Версия

- **Код версии**: 1
- **Название версии**: 1.0
- **Минимальный Android**: 8.0 (API 26)
- **Целевой Android**: 14.0 (API 35)
