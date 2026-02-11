package com.example.meteometar.data

/**
 * Данные NOTAM
 */
data class NotamData(
    val id: String,                    // Идентификатор NOTAM (например: A1234/25)
    val icao: String,                  // ICAO аэродрома
    val type: NotamType,               // Тип: NEW, REPLACE, CANCEL
    val category: NotamCategory,       // Категория
    val rawText: String,               // Исходный текст NOTAM
    val effectiveFrom: String,         // Начало действия
    val effectiveTo: String,           // Окончание действия
    val isPermanent: Boolean = false,  // Постоянный NOTAM
    val schedule: String? = null,      // Расписание
    val qCode: String? = null,         // Q-код
    val decoded: DecodedNotam? = null  // Расшифрованный NOTAM
)

/**
 * Расшифрованный NOTAM на русском языке
 */
data class DecodedNotam(
    val subject: String,         // Тема (ВПП, освещение, навигация...)
    val condition: String,       // Состояние (закрыто, ограничено...)
    val description: String,     // Полное описание
    val importance: NotamImportance  // Важность
)

/**
 * Тип NOTAM
 */
enum class NotamType {
    NEW,      // Новый (N)
    REPLACE,  // Замена (R)
    CANCEL    // Отмена (C)
}

/**
 * Категория NOTAM
 */
enum class NotamCategory {
    AERODROME,       // Аэродром
    RUNWAY,          // ВПП
    TAXIWAY,         // Рулёжные дорожки
    APRON,           // Перрон
    LIGHTING,        // Освещение
    NAVIGATION,      // Навигация (ILS, VOR, NDB, DME)
    COMMUNICATION,   // Связь
    AIRSPACE,        // Воздушное пространство
    OBSTACLE,        // Препятствия
    SERVICES,        // Услуги
    PROCEDURES,      // Процедуры
    OTHER            // Прочее
}

/**
 * Важность NOTAM
 */
enum class NotamImportance {
    CRITICAL,   // Критически важный (закрытие ВПП, аэродрома)
    WARNING,    // Предупреждение (ограничения)
    INFO        // Информационный
}

/**
 * Декодер NOTAM сообщений (на основе notam_decoder.py)
 */
object NotamDecoder {

    // Q-коды NOTAM (из Python скрипта)
    private val qCodes = mapOf(
        // Аэродромы (QF)
        "QFALC" to "аэродром закрыт",
        "QFALT" to "альтернативный аэродром доступен",
        "QFAXX" to "аэродром: прочее",
        "QFAAT" to "услуги аэродрома ограничены",
        "QFAHU" to "часы работы аэродрома изменены",

        // ВПП (QMR)
        "QMRLC" to "ВПП закрыта",
        "QMRLT" to "ВПП частично закрыта",
        "QMRXX" to "ВПП: прочее",
        "QMRAS" to "ВПП: длина сокращена",
        "QMRCC" to "ВПП: состояние покрытия изменено",
        "QMRDU" to "ВПП: загрязнение",
        "QMROW" to "ВПП: заблокирована",
        "QMRNG" to "ВПП: снижена прочность покрытия",
        "QMROB" to "ВПП: препятствие на полосе",
        "QMRST" to "ВПП: смещён порог",

        // Рулёжные дорожки (QMX)
        "QMXLC" to "рулёжная дорожка закрыта",
        "QMXLT" to "рулёжная дорожка частично закрыта",
        "QMXXX" to "рулёжная дорожка: прочее",
        "QMXAS" to "рулёжная дорожка: длина сокращена",
        "QMXOB" to "рулёжная дорожка: препятствие",

        // Перроны (QMA)
        "QMALC" to "перрон закрыт",
        "QMALT" to "перрон частично закрыт",
        "QMAXX" to "перрон: прочее",
        "QMAOB" to "перрон: препятствие",

        // Огни (QMG)
        "QMGXX" to "огни аэродрома: прочее",
        "QMGLU" to "огни ВПП не работают",
        "QMGLT" to "огни рулёжных дорожек не работают",
        "QMGLA" to "огни перрона не работают",
        "QMGAS" to "огни захода на посадку не работают",
        "QMGPD" to "PAPI не работает",
        "QMGVO" to "огни указателя ВПП не работают",

        // Навигация (QN)
        "QNIAS" to "ILS не работает",
        "QNIAT" to "ILS ограничено",
        "QNIXX" to "ILS: прочее",
        "QNVXX" to "VOR: прочее",
        "QNVAU" to "VOR не работает",
        "QNVAT" to "VOR ограничено",
        "QNDXX" to "DME: прочее",
        "QNDAU" to "DME не работает",
        "QNDAT" to "DME ограничено",
        "QNNXX" to "NDB: прочее",
        "QNNAU" to "NDB не работает",
        "QNNAT" to "NDB ограничено",
        "QNTAU" to "TACAN не работает",
        "QNHXX" to "маркерный радиомаяк: прочее",
        "QNGPS" to "GPS: помехи",
        "QNGXX" to "GNSS/GPS: прочее",

        // Воздушное пространство (QR)
        "QRRCA" to "ограничение воздушного пространства",
        "QRRCT" to "временное ограничение ВП",
        "QRPCA" to "запретная зона активна",
        "QRDCA" to "опасная зона активна",
        "QRTCA" to "временная зона активна",
        "QRAXX" to "воздушное пространство: прочее",
        "QRALT" to "изменение границ ВП",
        "QRMIL" to "военная зона активна",

        // Препятствия (QOB)
        "QOBXX" to "препятствие",
        "QOBCE" to "препятствие установлено",
        "QOBCL" to "препятствие освещено",
        "QOBRE" to "препятствие удалено",
        "QOBLT" to "огни препятствия не работают",

        // Связь (QC)
        "QCAXX" to "связь: прочее",
        "QCAAL" to "связь не работает",
        "QCAAS" to "диспетчерская связь не работает",
        "QCAHU" to "часы работы связи изменены",
        "QCASU" to "УКВ-связь не работает",

        // Процедуры (QP)
        "QPIAU" to "заход по приборам недоступен",
        "QPIAS" to "заход по приборам ограничен",
        "QPIXX" to "заход по приборам: прочее",
        "QPDXX" to "вылет по приборам: прочее",
        "QPFXX" to "процедуры полётов: прочее",

        // Услуги (QS)
        "QSAXX" to "аэронавигационное обслуживание: прочее",
        "QSFAU" to "топливо недоступно",
        "QSFAS" to "топливо ограничено",
        "QSGAU" to "противообледенение недоступно",
        "QSCAU" to "таможня закрыта",
        "QSIAU" to "иммиграция закрыта"
    )

    // Авиационные аббревиатуры для перевода
    private val abbreviations = mapOf(
        "RWY" to "ВПП",
        "RUNWAY" to "ВПП",
        "TWY" to "РД",
        "TAXIWAY" to "рулёжная дорожка",
        "APRON" to "перрон",
        "THR" to "порог ВПП",
        "THRESHOLD" to "порог ВПП",
        "CLSD" to "закрыт",
        "CLOSED" to "закрыт",
        "OPEN" to "открыт",
        "AVBL" to "доступен",
        "AVAILABLE" to "доступен",
        "U/S" to "не работает",
        "UNSERVICEABLE" to "не работает",
        "INOP" to "не работает",
        "INOPERATIVE" to "не работает",
        "LTD" to "ограничен",
        "LIMITED" to "ограничен",
        "MAINT" to "техобслуживание",
        "MAINTENANCE" to "техобслуживание",
        "WIP" to "строительные работы",
        "ILS" to "ILS",
        "VOR" to "VOR",
        "DME" to "DME",
        "NDB" to "NDB",
        "PAPI" to "PAPI",
        "LGT" to "огни",
        "LIGHTS" to "огни",
        "ALS" to "огни захода",
        "EDGE" to "кромочные огни",
        "CL" to "осевые огни",
        "SFC" to "поверхность",
        "GND" to "земля",
        "AGL" to "над землёй",
        "AMSL" to "над уровнем моря",
        "FT" to "фут",
        "FL" to "эшелон",
        "PERM" to "постоянно",
        "TEMPO" to "временно",
        "HR" to "час",
        "HRS" to "часов",
        "CTR" to "диспетчерская зона",
        "FIR" to "район полётной информации",
        "TMA" to "диспетчерский район",
        "FREQ" to "частота",
        "ATIS" to "ATIS",
        "APP" to "подход",
        "DEP" to "вылет",
        "ARR" to "прибытие",
        "SID" to "стандартный маршрут вылета",
        "STAR" to "стандартный маршрут прибытия",
        "ATC" to "УВД",
        "TWR" to "вышка",
        "FUEL" to "топливо",
        "DEICING" to "противообледенение",
        "EXP" to "ожидается",
        "EST" to "оценочно",
        "APPROX" to "приблизительно",
        "MAX" to "макс.",
        "MIN" to "мин.",
        "INCL" to "включительно",
        "EXC" to "исключая",
        "EXCEPT" to "исключая",
        "DUE" to "из-за",
        "DUE TO" to "из-за"
    )

    /**
     * Декодировать NOTAM
     */
    fun decode(rawNotam: String, qCode: String? = null): DecodedNotam {
        val upperNotam = rawNotam.uppercase()

        // Определяем предмет по Q-коду или тексту
        val subject = if (qCode != null) {
            qCodes[qCode] ?: findSubjectFromText(upperNotam)
        } else {
            findSubjectFromText(upperNotam)
        }

        // Определяем условие
        val condition = findCondition(upperNotam)

        // Определяем важность
        val importance = determineImportance(qCode, upperNotam, subject, condition)

        // Формируем описание
        val description = buildDescription(rawNotam)

        return DecodedNotam(
            subject = subject,
            condition = condition,
            description = description,
            importance = importance
        )
    }

    private fun findSubjectFromText(notam: String): String {
        return when {
            notam.contains("RWY") || notam.contains("RUNWAY") -> "ВПП"
            notam.contains("TWY") || notam.contains("TAXIWAY") -> "рулёжная дорожка"
            notam.contains("APRON") -> "перрон"
            notam.contains("ILS") -> "ILS"
            notam.contains("VOR") -> "VOR"
            notam.contains("NDB") -> "NDB"
            notam.contains("DME") -> "DME"
            notam.contains("PAPI") || notam.contains("VASI") -> "PAPI"
            notam.contains("LGT") || notam.contains("LIGHT") -> "освещение"
            notam.contains("TWR") -> "диспетчерская вышка"
            notam.contains("FUEL") -> "топливозаправка"
            notam.contains("OBST") || notam.contains("CRANE") -> "препятствие"
            notam.contains("AD ") || notam.contains("AERODROME") -> "аэродром"
            notam.contains("CTR") || notam.contains("TMA") || notam.contains("FIR") -> "воздушное пространство"
            notam.contains("SID") || notam.contains("STAR") -> "процедуры"
            notam.contains("FREQ") || notam.contains("ATIS") -> "связь"
            else -> "информация"
        }
    }

    private fun findCondition(notam: String): String {
        return when {
            notam.contains("CLSD") || notam.contains("CLOSED") -> "закрыто"
            notam.contains("U/S") || notam.contains("UNSERVICEABLE") || notam.contains("INOP") -> "не работает"
            notam.contains("LIMITED") || notam.contains("LTD") -> "ограничено"
            notam.contains("AVBL") || notam.contains("AVAILABLE") -> "доступно"
            notam.contains("ERECTED") -> "установлено"
            notam.contains("REMOVED") -> "снято"
            notam.contains("WIP") || notam.contains("WORK IN PROGRESS") -> "идут работы"
            notam.contains("MAINT") || notam.contains("MAINTENANCE") -> "техобслуживание"
            notam.contains("TEST") -> "испытания"
            notam.contains("NOT AVBL") -> "не доступно"
            notam.contains("SUSPENDED") -> "приостановлено"
            notam.contains("ACTIVE") || notam.contains("ACT") -> "активно"
            else -> "действует"
        }
    }

    private fun determineImportance(qCode: String?, notam: String, subject: String, condition: String): NotamImportance {
        // Критичные Q-коды
        val criticalQCodes = listOf("QFALC", "QMRLC", "QMXLC", "QNIAS")
        if (qCode != null && qCode in criticalQCodes) {
            return NotamImportance.CRITICAL
        }

        // Критические случаи по тексту
        if (condition in listOf("закрыто") && subject in listOf("ВПП", "аэродром")) {
            return NotamImportance.CRITICAL
        }

        if (notam.contains("CLSD") && (notam.contains("RWY") || notam.contains("AD "))) {
            return NotamImportance.CRITICAL
        }

        // Предупреждающие Q-коды
        val warningQCodes = listOf("QMRLT", "QMALC", "QMGLU", "QRRCA", "QOBXX")
        if (qCode != null && warningQCodes.any { qCode.startsWith(it.take(4)) }) {
            return NotamImportance.WARNING
        }

        // Предупреждения по тексту
        if (condition in listOf("не работает", "ограничено", "идут работы") ||
            subject in listOf("ILS", "VOR", "NDB", "освещение", "PAPI")) {
            return NotamImportance.WARNING
        }

        return NotamImportance.INFO
    }

    private fun buildDescription(rawNotam: String): String {
        val sb = StringBuilder()

        // Извлекаем секцию E) - основной текст
        val ePattern = Regex("E\\)\\s*(.+?)(?=\\s*[FG]\\)|$)", RegexOption.DOT_MATCHES_ALL)
        val eMatch = ePattern.find(rawNotam)
        if (eMatch != null) {
            val eText = translateAbbreviations(eMatch.groupValues[1].trim())
            sb.append(eText)
        } else {
            // Если нет секции E), переводим весь текст
            sb.append(translateAbbreviations(rawNotam))
        }

        return sb.toString().trim()
    }

    private fun translateAbbreviations(text: String): String {
        var result = text

        for ((eng, rus) in abbreviations) {
            val pattern = Regex("\\b${Regex.escape(eng)}\\b", RegexOption.IGNORE_CASE)
            result = pattern.replace(result, rus)
        }

        return result
    }

    /**
     * Определить категорию NOTAM по Q-коду или тексту
     */
    fun determineCategory(qCode: String?, rawNotam: String): NotamCategory {
        // По Q-коду
        if (qCode != null) {
            return when {
                qCode.startsWith("QFA") -> NotamCategory.AERODROME
                qCode.startsWith("QMR") -> NotamCategory.RUNWAY
                qCode.startsWith("QMX") -> NotamCategory.TAXIWAY
                qCode.startsWith("QMA") -> NotamCategory.APRON
                qCode.startsWith("QMG") -> NotamCategory.LIGHTING
                qCode.startsWith("QN") -> NotamCategory.NAVIGATION
                qCode.startsWith("QC") -> NotamCategory.COMMUNICATION
                qCode.startsWith("QR") -> NotamCategory.AIRSPACE
                qCode.startsWith("QOB") -> NotamCategory.OBSTACLE
                qCode.startsWith("QS") -> NotamCategory.SERVICES
                qCode.startsWith("QP") -> NotamCategory.PROCEDURES
                else -> NotamCategory.OTHER
            }
        }

        // По тексту
        val upperNotam = rawNotam.uppercase()
        return when {
            upperNotam.contains("RWY") -> NotamCategory.RUNWAY
            upperNotam.contains("TWY") || upperNotam.contains("TAXIWAY") -> NotamCategory.TAXIWAY
            upperNotam.contains("APRON") -> NotamCategory.APRON
            upperNotam.contains("LGT") || upperNotam.contains("LIGHT") ||
            upperNotam.contains("PAPI") || upperNotam.contains("VASI") -> NotamCategory.LIGHTING
            upperNotam.contains("ILS") || upperNotam.contains("VOR") ||
            upperNotam.contains("NDB") || upperNotam.contains("DME") ||
            upperNotam.contains("TACAN") -> NotamCategory.NAVIGATION
            upperNotam.contains("TWR") || upperNotam.contains("FREQ") ||
            upperNotam.contains("ATIS") -> NotamCategory.COMMUNICATION
            upperNotam.contains("CTR") || upperNotam.contains("TMA") ||
            upperNotam.contains("FIR") || upperNotam.contains("AIRSPACE") -> NotamCategory.AIRSPACE
            upperNotam.contains("OBST") || upperNotam.contains("CRANE") ||
            upperNotam.contains("TOWER") -> NotamCategory.OBSTACLE
            upperNotam.contains("SID") || upperNotam.contains("STAR") ||
            upperNotam.contains("PROC") -> NotamCategory.PROCEDURES
            upperNotam.contains("AD ") || upperNotam.contains("AERODROME") -> NotamCategory.AERODROME
            else -> NotamCategory.OTHER
        }
    }

    /**
     * Парсинг даты NOTAM (YYMMDDHHmm -> DD.MM.YY HH:MM)
     */
    fun parseNotamDateTime(dtStr: String): String {
        if (dtStr.length != 10) return dtStr

        return try {
            val yy = dtStr.substring(0, 2)
            val mm = dtStr.substring(2, 4)
            val dd = dtStr.substring(4, 6)
            val hh = dtStr.substring(6, 8)
            val min = dtStr.substring(8, 10)
            "$dd.$mm.$yy $hh:$min UTC"
        } catch (e: Exception) {
            dtStr
        }
    }
}
