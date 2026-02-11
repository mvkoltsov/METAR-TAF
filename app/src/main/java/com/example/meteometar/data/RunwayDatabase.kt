package com.example.meteometar.data

/**
 * Данные о взлётно-посадочной полосе
 */
data class RunwayData(
    val name: String,           // Название ВПП (например "05/23")
    val course1: Int,           // Курс первого направления (05 = 050°)
    val course2: Int,           // Курс второго направления (23 = 230°)
    val lengthM: Int? = null,   // Длина в метрах
    val widthM: Int? = null,    // Ширина в метрах
    val surface: String? = null // Покрытие (асфальт, бетон и т.д.)
) {
    /**
     * Получить активную ВПП на основе направления ветра
     * Возвращает курс ВПП, который наиболее близок к направлению ветра (против ветра)
     */
    fun getActiveRunway(windDirection: Int?): String {
        if (windDirection == null) return name.split("/").first()

        // Извлекаем номера ВПП из названия (например "11/29" -> "11" и "29")
        val parts = name.replace("L", "").replace("R", "").replace("C", "").split("/")
        val rwy1 = parts.getOrNull(0) ?: return name.split("/").first()
        val rwy2 = parts.getOrNull(1) ?: return rwy1

        // Определяем какой курс ближе к направлению ветра (взлёт против ветра)
        val diff1 = angleDifference(course1, windDirection)
        val diff2 = angleDifference(course2, windDirection)

        return if (diff1 <= diff2) rwy1 else rwy2
    }

    private fun angleDifference(angle1: Int, angle2: Int): Int {
        var diff = kotlin.math.abs(angle1 - angle2) % 360
        if (diff > 180) diff = 360 - diff
        return diff
    }
}

/**
 * База данных ВПП аэропортов
 */
object RunwayDatabase {

    /**
     * Карта ICAO -> список ВПП
     */
    private val runways: Map<String, List<RunwayData>> = mapOf(
        // ===== КАЗАХСТАН (100% охват) =====

        // Алматы (UAAA)
        "UAAA" to listOf(
            RunwayData("05R/23L", 52, 232, 4500, 45, "асфальтобетон"),
            RunwayData("05L/23R", 52, 232, 4401, 60, "асфальтобетон")
        ),

        // Астана (UACC)
        "UACC" to listOf(
            RunwayData("04/22", 40, 220, 3500, 45, "бетон"),
            RunwayData("16/34", 164, 344, 3500, 45, "бетон")
        ),

        // Актау (UATE)
        "UATE" to listOf(
            RunwayData("13/31", 130, 310, 3000, 48, "асфальтобетон")
        ),

        // Актобе (UATT)
        "UATT" to listOf(
            RunwayData("13/31", 130, 310, 2808, 42, "асфальтобетон")
        ),

        // Атырау (UATG)
        "UATG" to listOf(
            RunwayData("11/29", 107, 287, 3002, 45, "бетон")
        ),

        // Балхаш (UAAH)
        "UAAH" to listOf(
            RunwayData("03/21", 30, 210, 2500, 42, "бетон")
        ),

        // Караганда (UAKK)
        "UAKK" to listOf(
            RunwayData("02/20", 18, 198, 3200, 60, "бетон")
        ),

        // Кокшетау (UACK)
        "UACK" to listOf(
            RunwayData("17/35", 170, 350, 2500, 42, "асфальтобетон")
        ),

        // Костанай (UAUU)
        "UAUU" to listOf(
            RunwayData("04/22", 42, 222, 2800, 42, "бетон")
        ),

        // Кызылорда (UAOO)
        "UAOO" to listOf(
            RunwayData("08/26", 80, 260, 3000, 45, "асфальтобетон")
        ),

        // Павлодар (UASP)
        "UASP" to listOf(
            RunwayData("16/34", 160, 340, 2500, 45, "бетон")
        ),

        // Петропавловск (UACP)
        "UACP" to listOf(
            RunwayData("09/27", 90, 270, 2500, 42, "асфальтобетон")
        ),

        // Семей (UASS)
        "UASS" to listOf(
            RunwayData("16/34", 160, 340, 2500, 43, "бетон")
        ),

        // Шымкент (UAII)
        "UAII" to listOf(
            RunwayData("10/28", 98, 278, 3498, 45, "бетон")
        ),

        // Талдыкорган (UAAT)
        "UAAT" to listOf(
            RunwayData("08/26", 84, 264, 2200, 35, "асфальт")
        ),

        // Тараз (UADD)
        "UADD" to listOf(
            RunwayData("08/26", 80, 260, 3200, 45, "бетон")
        ),

        // Туркестан (UAIT)
        "UAIT" to listOf(
            RunwayData("09/27", 90, 270, 3000, 45, "бетон")
        ),

        // Уральск (UARR)
        "UARR" to listOf(
            RunwayData("02/20", 20, 200, 2506, 42, "бетон")
        ),

        // Усть-Каменогорск (UASK)
        "UASK" to listOf(
            RunwayData("05/23", 52, 232, 2800, 45, "бетон")
        ),

        // Жезказган (UAKD)
        "UAKD" to listOf(
            RunwayData("17/35", 170, 350, 2800, 42, "бетон")
        ),

        // Зайсан (UASZ)
        "UASZ" to listOf(
            RunwayData("14/32", 140, 320, 1800, 30, "грунт")
        ),

        // Тенгиз (UATZ) - служебный аэродром
        "UATZ" to listOf(
            RunwayData("14/32", 140, 320, 2440, 45, "асфальтобетон")
        ),

        // ===== РОССИЯ =====

        // Москва Шереметьево (UUEE)
        "UUEE" to listOf(
            RunwayData("06L/24R", 64, 244, 3550, 60, "бетон"),
            RunwayData("06C/24C", 64, 244, 3200, 60, "бетон"),
            RunwayData("06R/24L", 64, 244, 3700, 60, "бетон")
        ),

        // Москва Домодедово (UUDD)
        "UUDD" to listOf(
            RunwayData("14L/32R", 144, 324, 3794, 53, "бетон"),
            RunwayData("14R/32L", 144, 324, 3500, 46, "бетон")
        ),

        // Москва Внуково (UUWW)
        "UUWW" to listOf(
            RunwayData("01/19", 6, 186, 3060, 60, "бетон"),
            RunwayData("06/24", 64, 244, 3000, 60, "бетон")
        ),

        // Санкт-Петербург Пулково (ULLI)
        "ULLI" to listOf(
            RunwayData("10L/28R", 103, 283, 3782, 60, "бетон"),
            RunwayData("10R/28L", 103, 283, 3393, 60, "бетон")
        ),

        // Новосибирск Толмачёво (UNNT)
        "UNNT" to listOf(
            RunwayData("07/25", 71, 251, 3600, 60, "бетон"),
            RunwayData("16/34", 163, 343, 3358, 45, "бетон")
        ),

        // Екатеринбург Кольцово (USSS)
        "USSS" to listOf(
            RunwayData("08L/26R", 82, 262, 3000, 57, "бетон"),
            RunwayData("08R/26L", 82, 262, 3021, 45, "бетон")
        ),

        // Сочи Адлер (URSS)
        "URSS" to listOf(
            RunwayData("02/20", 24, 204, 2500, 49, "бетон"),
            RunwayData("06/24", 60, 240, 3000, 60, "бетон")
        ),

        // Казань (UWKD)
        "UWKD" to listOf(
            RunwayData("11/29", 110, 290, 3750, 60, "бетон")
        ),

        // Самара (UWWW)
        "UWWW" to listOf(
            RunwayData("05/23", 50, 230, 3001, 45, "бетон")
        ),

        // Омск (UNOO)
        "UNOO" to listOf(
            RunwayData("07/25", 72, 252, 2502, 42, "асфальтобетон")
        ),

        // Тюмень (USTO)
        "USTO" to listOf(
            RunwayData("03/21", 30, 210, 2800, 45, "бетон")
        ),

        // Челябинск (USCC)
        "USCC" to listOf(
            RunwayData("09/27", 87, 267, 3200, 60, "бетон")
        ),

        // Оренбург (UWOO)
        "UWOO" to listOf(
            RunwayData("07/25", 72, 252, 2502, 42, "бетон")
        ),

        // Уфа (UWUU)
        "UWUU" to listOf(
            RunwayData("14/32", 140, 320, 2873, 57, "бетон")
        ),

        // ===== УЗБЕКИСТАН =====

        // Ташкент (UTTT)
        "UTTT" to listOf(
            RunwayData("08/26", 80, 260, 4000, 50, "бетон")
        ),

        // Самарканд (UTSS)
        "UTSS" to listOf(
            RunwayData("09/27", 90, 270, 3100, 45, "бетон")
        ),

        // Бухара (UTSB)
        "UTSB" to listOf(
            RunwayData("10/28", 100, 280, 3000, 45, "бетон")
        ),

        // ===== КЫРГЫЗСТАН =====

        // Бишкек Манас (UCFM)
        "UCFM" to listOf(
            RunwayData("08/26", 80, 260, 4200, 50, "бетон")
        ),

        // Ош (UCFO)
        "UCFO" to listOf(
            RunwayData("12/30", 122, 302, 2652, 44, "асфальтобетон")
        ),

        // ===== ТУРКМЕНИСТАН =====

        // Ашхабад (UTAA)
        "UTAA" to listOf(
            RunwayData("12/30", 120, 300, 3800, 60, "бетон")
        ),

        // ===== ТАДЖИКИСТАН =====

        // Душанбе (UTDD)
        "UTDD" to listOf(
            RunwayData("09/27", 90, 270, 3100, 50, "бетон")
        ),

        // ===== АЗЕРБАЙДЖАН =====

        // Баку (UBBB)
        "UBBB" to listOf(
            RunwayData("16/34", 160, 340, 3200, 60, "бетон"),
            RunwayData("18/36", 180, 360, 4500, 60, "бетон")
        ),

        // ===== ГРУЗИЯ =====

        // Тбилиси (UGTB)
        "UGTB" to listOf(
            RunwayData("13L/31R", 130, 310, 3000, 45, "бетон"),
            RunwayData("13R/31L", 130, 310, 2700, 45, "бетон")
        ),

        // ===== АРМЕНИЯ =====

        // Ереван Звартноц (UDYZ)
        "UDYZ" to listOf(
            RunwayData("09/27", 90, 270, 3850, 55, "бетон")
        ),

        // ===== ТУРЦИЯ =====

        // Стамбул (LTFM)
        "LTFM" to listOf(
            RunwayData("16L/34R", 159, 339, 4100, 60, "асфальт"),
            RunwayData("16R/34L", 159, 339, 3750, 60, "асфальт"),
            RunwayData("17L/35R", 168, 348, 4100, 60, "асфальт"),
            RunwayData("17R/35L", 168, 348, 3750, 60, "асфальт")
        ),

        // Анталья (LTAI)
        "LTAI" to listOf(
            RunwayData("18L/36R", 180, 360, 3400, 45, "асфальт"),
            RunwayData("18R/36L", 180, 360, 3400, 45, "асфальт")
        ),

        // ===== КИТАЙ =====

        // Урумчи (ZWWW)
        "ZWWW" to listOf(
            RunwayData("07/25", 70, 250, 3600, 45, "бетон"),
            RunwayData("12/30", 123, 303, 3200, 45, "бетон")
        ),

        // ===== МОНГОЛИЯ =====

        // Улан-Батор (ZMUB)
        "ZMUB" to listOf(
            RunwayData("14/32", 140, 320, 3100, 45, "бетон")
        ),

        // ===== ИРАН =====

        // Тегеран Имам Хомейни (OIIE)
        "OIIE" to listOf(
            RunwayData("11L/29R", 110, 290, 4200, 60, "асфальт"),
            RunwayData("11R/29L", 110, 290, 4200, 60, "асфальт")
        )
    )

    /**
     * Получить список ВПП для аэропорта
     */
    fun getRunways(icao: String): List<RunwayData> {
        return runways[icao.uppercase()] ?: emptyList()
    }

    /**
     * Получить информацию о ВПП в текстовом виде
     */
    fun getRunwayInfo(icao: String): String {
        val rwyList = getRunways(icao)
        if (rwyList.isEmpty()) return ""

        return rwyList.joinToString(" | ") { it.name }
    }

    /**
     * Получить активную ВПП на основе направления ветра
     */
    fun getActiveRunway(icao: String, windDirection: Int?): String? {
        val rwyList = getRunways(icao)
        if (rwyList.isEmpty()) return null

        // Берём первую (основную) ВПП
        return rwyList.first().getActiveRunway(windDirection)
    }

    /**
     * Получить детальную информацию о ВПП
     */
    fun getRunwayDetails(icao: String): List<String> {
        val rwyList = getRunways(icao)
        return rwyList.map { rwy ->
            buildString {
                append("ВПП ${rwy.name}")
                rwy.lengthM?.let { append(" • ${it}м") }
                rwy.widthM?.let { append("×${it}м") }
                rwy.surface?.let { append(" • $it") }
            }
        }
    }
}
