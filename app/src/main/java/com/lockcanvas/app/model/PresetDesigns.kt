package com.lockcanvas.app.model

object PresetDesigns {

    const val ID_MINIMAL_CLOCK = "preset_minimal_clock_battery"
    const val ID_CYBER_NEON = "preset_cyber_neon_gauge"
    const val ID_NIGHT_ZEN = "preset_night_zen_canvas"
    const val ID_MONO_TYPE = "preset_mono_type"
    const val ID_SUNSET_GRADIENT = "preset_sunset_gradient"
    const val ID_PAPER_INK = "preset_paper_ink"
    const val ID_KOYOMI = "preset_koyomi"
    const val ID_SORAMOYO = "preset_soramoyo"
    const val ID_NEXT_EVENT = "preset_next_event"

    fun all(): List<CanvasDesign> = listOf(
        minimalClockAndBattery(),
        monoType(),
        sunsetGradient(),
        cyberNeonGauge(),
        nightZenCanvas(),
        paperInk(),
        koyomi(),
        soramoyo(),
        nextEvent(),
    )

    fun defaultDesign(): CanvasDesign = minimalClockAndBattery()

    fun minimalClockAndBattery(): CanvasDesign = CanvasDesign(
        id = ID_MINIMAL_CLOCK,
        name = "オーロラガラス",
        backgroundColorArgb = 0xFF0B1030,
        secondaryBackgroundColorArgb = 0xFF0E3D43,
        backgroundGradientEnabled = true,
        animationStyle = AnimationStyle.NONE,
        elements = listOf(
            ImageElement(
                id = "min_bg",
                xFraction = 0.5f,
                yFraction = 0.5f,
                uriString = "android.resource://com.lockcanvas.app/drawable/bg_aurora",
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                cornerRadiusDp = 0f,
                zIndex = -10,
            ),
            ShapeElement(
                id = "min_card",
                xFraction = 0.5f,
                yFraction = 0.525f,
                widthFraction = 0.84f,
                heightFraction = 0.295f,
                shapeType = ShapeType.RECTANGLE,
                fillColorArgb = 0x2EFFFFFF,
                strokeColorArgb = 0x66C4B5FD,
                strokeWidthDp = 1.5f,
                cornerRadiusDp = 32f,
                zIndex = 0,
            ),
            TextElement(
                id = "min_date",
                xFraction = 0.5f,
                yFraction = 0.445f,
                text = "{date}",
                colorArgb = 0xFFC4B5FD,
                fontSizeSp = 16f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "min_time",
                xFraction = 0.5f,
                yFraction = 0.510f,
                text = "{time}",
                colorArgb = 0xFFF8FAFC,
                fontSizeSp = 64f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            BatteryGaugeElement(
                id = "min_battery_bar",
                xFraction = 0.5f,
                yFraction = 0.583f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.66f,
                heightFraction = 0.030f,
                activeColorArgb = 0xFF7DD3FC,
                trackColorArgb = 0x2EFFFFFF,
                textColorArgb = 0xFFF8FAFC,
                showLabel = true,
                zIndex = 12,
            ),
            TextElement(
                id = "min_status",
                xFraction = 0.5f,
                yFraction = 0.630f,
                text = "{charging_status} · {battery}",
                colorArgb = 0xFFA5B4FC,
                fontSizeSp = 13f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 13,
            ),
        ),
        rules = listOf(
            ConditionalRule(
                id = "min_rule_low_battery",
                name = "電池が少ないとき知らせる",
                enabled = true,
                condition = RuleCondition.BatteryBelow(thresholdPercent = 20),
                action = RuleAction.OverrideElementText(
                    elementId = "min_status",
                    text = "LOW POWER · {battery}",
                ),
            ),
            ConditionalRule(
                id = "min_rule_low_bg",
                name = "電池が少ないとき背景を赤く",
                enabled = true,
                condition = RuleCondition.BatteryBelow(thresholdPercent = 15),
                action = RuleAction.OverrideBackground(colorArgb = 0xFF2B0E17),
            ),
        ),
        updatedAtEpochMs = 1_700_000_000_000L,
    )

    fun cyberNeonGauge(): CanvasDesign = CanvasDesign(
        id = ID_CYBER_NEON,
        name = "ネオンHUD",
        backgroundColorArgb = 0xFF05040F,
        secondaryBackgroundColorArgb = 0xFF1B0B38,
        backgroundGradientEnabled = true,
        animationStyle = AnimationStyle.FLOATING_PARTICLES,
        elements = listOf(
            ImageElement(
                id = "cyber_bg",
                xFraction = 0.5f,
                yFraction = 0.5f,
                uriString = "android.resource://com.lockcanvas.app/drawable/bg_neon",
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                cornerRadiusDp = 0f,
                zIndex = -10,
            ),
            ShapeElement(
                id = "cyber_frame",
                xFraction = 0.5f,
                yFraction = 0.50f,
                widthFraction = 0.94f,
                heightFraction = 0.92f,
                shapeType = ShapeType.RECTANGLE,
                fillColorArgb = 0x00000000,
                strokeColorArgb = 0x55A855F7,
                strokeWidthDp = 1.2f,
                cornerRadiusDp = 20f,
                zIndex = 1,
            ),
            ShapeElement(
                id = "cyber_ring_backdrop",
                xFraction = 0.5f,
                yFraction = 0.50f,
                widthFraction = 0.58f,
                heightFraction = 0.27f,
                shapeType = ShapeType.CIRCLE,
                fillColorArgb = 0x1600F5D4,
                strokeColorArgb = 0x5500F5D4,
                strokeWidthDp = 1.5f,
                cornerRadiusDp = 0f,
                zIndex = 0,
            ),
            BatteryGaugeElement(
                id = "cyber_gauge",
                xFraction = 0.5f,
                yFraction = 0.50f,
                style = BatteryGaugeStyle.CIRCULAR,
                widthFraction = 0.50f,
                heightFraction = 0.235f,
                activeColorArgb = 0xFF00F5D4,
                trackColorArgb = 0x33A855F7,
                textColorArgb = 0xFFF0FDFA,
                showLabel = true,
                zIndex = 10,
            ),
            ShapeElement(
                id = "cyber_pill",
                xFraction = 0.5f,
                yFraction = 0.675f,
                widthFraction = 0.68f,
                heightFraction = 0.055f,
                shapeType = ShapeType.PILL,
                fillColorArgb = 0x2EA855F7,
                strokeColorArgb = 0xAAC084FC,
                strokeWidthDp = 1.2f,
                cornerRadiusDp = 10f,
                zIndex = 5,
            ),
            TextElement(
                id = "cyber_status",
                xFraction = 0.5f,
                yFraction = 0.675f,
                text = "{time}  |  {charging_status}",
                colorArgb = 0xFFE9D5FF,
                fontSizeSp = 14f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 12,
            ),
            TextElement(
                id = "cyber_charge_banner",
                xFraction = 0.5f,
                yFraction = 0.345f,
                text = "⚡ SUPERCHARGE ACTIVE",
                colorArgb = 0xFF00F5D4,
                fontSizeSp = 13f,
                bold = true,
                dynamicTemplate = false,
                visible = false,
                zIndex = 14,
            ),
        ),
        rules = listOf(
            ConditionalRule(
                id = "cyber_rule_charging_badge",
                name = "充電中にバナーを出す",
                enabled = true,
                condition = RuleCondition.IsCharging,
                action = RuleAction.ToggleElementVisibility(
                    elementId = "cyber_charge_banner",
                    visible = true,
                ),
            ),
        ),
        updatedAtEpochMs = 1_700_000_001_000L,
    )

    fun nightZenCanvas(): CanvasDesign = CanvasDesign(
        id = ID_NIGHT_ZEN,
        name = "満月の禅",
        backgroundColorArgb = 0xFF0A120E,
        secondaryBackgroundColorArgb = 0xFF16241D,
        backgroundGradientEnabled = true,
        animationStyle = AnimationStyle.PULSE_GLOW,
        elements = listOf(
            ImageElement(
                id = "zen_bg",
                xFraction = 0.5f,
                yFraction = 0.5f,
                uriString = "android.resource://com.lockcanvas.app/drawable/bg_zen",
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                cornerRadiusDp = 0f,
                zIndex = -10,
            ),
            TextElement(
                id = "zen_quote",
                xFraction = 0.5f,
                yFraction = 0.462f,
                text = "Stay Present",
                colorArgb = 0xFFE9FDF3,
                fontSizeSp = 26f,
                bold = false,
                dynamicTemplate = false,
                zIndex = 10,
            ),
            TextElement(
                id = "zen_meta",
                xFraction = 0.5f,
                yFraction = 0.527f,
                text = "{date} · {time}",
                colorArgb = 0xFF99F6E4,
                fontSizeSp = 15f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            BatteryGaugeElement(
                id = "zen_battery",
                xFraction = 0.5f,
                yFraction = 0.578f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.36f,
                heightFraction = 0.016f,
                activeColorArgb = 0xFF6EE7B7,
                trackColorArgb = 0x26FFFFFF,
                textColorArgb = 0xFFD1FAE5,
                showLabel = true,
                zIndex = 12,
            ),
        ),
        rules = listOf(
            ConditionalRule(
                id = "zen_rule_night_bg",
                name = "夜は背景を暗く",
                enabled = true,
                condition = RuleCondition.NightTime(startHour = 21, endHour = 6),
                action = RuleAction.OverrideBackground(colorArgb = 0xFF06090B),
            ),
            ConditionalRule(
                id = "zen_rule_night_text",
                name = "夜のあいさつ",
                enabled = true,
                condition = RuleCondition.NightTime(startHour = 21, endHour = 6),
                action = RuleAction.OverrideElementText(
                    elementId = "zen_quote",
                    text = "Rest Easy",
                ),
            ),
        ),
        updatedAtEpochMs = 1_700_000_002_000L,
    )

    fun monoType(): CanvasDesign = CanvasDesign(
        id = ID_MONO_TYPE,
        name = "モノタイプ",
        backgroundColorArgb = 0xFF000000,
        secondaryBackgroundColorArgb = 0xFF171717,
        backgroundGradientEnabled = true,
        animationStyle = AnimationStyle.NONE,
        elements = listOf(
            TextElement(
                id = "mono_date",
                xFraction = 0.5f,
                yFraction = 0.292f,
                text = "{weekday} · {date}",
                colorArgb = 0xFF9CA3AF,
                fontSizeSp = 14f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "mono_time",
                xFraction = 0.5f,
                yFraction = 0.420f,
                text = "{time}",
                colorArgb = 0xFFFFFFFF,
                fontSizeSp = 92f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            ShapeElement(
                id = "mono_divider",
                xFraction = 0.5f,
                yFraction = 0.505f,
                widthFraction = 0.20f,
                heightFraction = 0.0022f,
                shapeType = ShapeType.RECTANGLE,
                fillColorArgb = 0x4DFFFFFF,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 1f,
                zIndex = 5,
            ),
            TextElement(
                id = "mono_battery",
                xFraction = 0.5f,
                yFraction = 0.548f,
                text = "{battery} · {charging_status}",
                colorArgb = 0xFF9CA3AF,
                fontSizeSp = 12f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 12,
            ),
        ),
        rules = emptyList(),
        updatedAtEpochMs = 1_700_000_003_000L,
    )

    fun sunsetGradient(): CanvasDesign = CanvasDesign(
        id = ID_SUNSET_GRADIENT,
        name = "夕焼けグラデ",
        backgroundColorArgb = 0xFFB34724,
        secondaryBackgroundColorArgb = 0xFFF3D9CF,
        backgroundGradientEnabled = true,
        animationStyle = AnimationStyle.NONE,
        elements = listOf(
            ImageElement(
                id = "sunset_bg",
                xFraction = 0.5f,
                yFraction = 0.5f,
                uriString = "android.resource://com.lockcanvas.app/drawable/bg_sunset",
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                cornerRadiusDp = 0f,
                zIndex = -10,
            ),
            TextElement(
                id = "sunset_date",
                xFraction = 0.5f,
                yFraction = 0.292f,
                text = "{date} · Golden Hour",
                colorArgb = 0xFFFFF7ED,
                fontSizeSp = 13f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "sunset_time",
                xFraction = 0.5f,
                yFraction = 0.358f,
                text = "{time}",
                colorArgb = 0xFFFFFFFF,
                fontSizeSp = 46f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            BatteryGaugeElement(
                id = "sunset_battery",
                xFraction = 0.5f,
                yFraction = 0.795f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.50f,
                heightFraction = 0.024f,
                activeColorArgb = 0xFFFFF7ED,
                trackColorArgb = 0x40FFFFFF,
                textColorArgb = 0xFF7C2D12,
                showLabel = true,
                zIndex = 12,
            ),
        ),
        rules = emptyList(),
        updatedAtEpochMs = 1_700_000_004_000L,
    )

    fun paperInk(): CanvasDesign = CanvasDesign(
        id = ID_PAPER_INK,
        name = "和紙インク",
        backgroundColorArgb = 0xFFF5EFE2,
        secondaryBackgroundColorArgb = 0xFFEAE0CC,
        backgroundGradientEnabled = true,
        animationStyle = AnimationStyle.NONE,
        elements = listOf(
            ImageElement(
                id = "ink_bg",
                xFraction = 0.5f,
                yFraction = 0.5f,
                uriString = "android.resource://com.lockcanvas.app/drawable/bg_washi",
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                cornerRadiusDp = 0f,
                zIndex = -10,
            ),
            TextElement(
                id = "ink_date",
                xFraction = 0.5f,
                yFraction = 0.298f,
                text = "{date}",
                colorArgb = 0xFF3E362C,
                fontSizeSp = 14f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "ink_time",
                xFraction = 0.5f,
                yFraction = 0.398f,
                text = "{time}",
                colorArgb = 0xFF2B2620,
                fontSizeSp = 64f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            TextElement(
                id = "ink_quote",
                xFraction = 0.5f,
                yFraction = 0.482f,
                text = "静かな朝",
                colorArgb = 0xFF6B5D4F,
                fontSizeSp = 16f,
                bold = false,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            ShapeElement(
                id = "ink_seal",
                xFraction = 0.795f,
                yFraction = 0.600f,
                widthFraction = 0.105f,
                heightFraction = 0.050f,
                shapeType = ShapeType.RECTANGLE,
                fillColorArgb = 0xFFB3382C,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 4f,
                zIndex = 5,
            ),
            TextElement(
                id = "ink_seal_text",
                xFraction = 0.795f,
                yFraction = 0.600f,
                text = "大",
                colorArgb = 0xFFFFFFFF,
                fontSizeSp = 13f,
                bold = true,
                dynamicTemplate = false,
                zIndex = 6,
            ),
            BatteryGaugeElement(
                id = "ink_battery",
                xFraction = 0.5f,
                yFraction = 0.790f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.36f,
                heightFraction = 0.018f,
                activeColorArgb = 0xFF8A7A63,
                trackColorArgb = 0x333E362C,
                textColorArgb = 0xFF6B5D4F,
                showLabel = true,
                zIndex = 13,
            ),
        ),
        rules = listOf(
            ConditionalRule(
                id = "ink_rule_night_quote",
                name = "夜は言葉を変える",
                enabled = true,
                condition = RuleCondition.NightTime(startHour = 21, endHour = 6),
                action = RuleAction.OverrideElementText(
                    elementId = "ink_quote",
                    text = "静かな夜",
                ),
            ),
        ),
        updatedAtEpochMs = 1_700_000_005_000L,
    )

    /** The 24 solar terms: (name, seasonal word, start month, start day). */
    private data class Sekki(val name: String, val word: String, val month: Int, val day: Int)

    private val sekkiTable = listOf(
        Sekki("小寒", "寒の入り", 1, 6),
        Sekki("大寒", "一年でいちばん寒い頃", 1, 20),
        Sekki("立春", "春立つ", 2, 4),
        Sekki("雨水", "雪が雨に変わる頃", 2, 19),
        Sekki("啓蟄", "虫が顔を出す頃", 3, 6),
        Sekki("春分", "昼と夜が同じ長さ", 3, 21),
        Sekki("清明", "空が澄んでくる頃", 4, 5),
        Sekki("穀雨", "雨が穀物をうるおす", 4, 20),
        Sekki("立夏", "夏の気配", 5, 6),
        Sekki("小満", "緑が満ちてくる", 5, 21),
        Sekki("芒種", "稲を植える頃", 6, 6),
        Sekki("夏至", "一年でいちばん昼が長い", 6, 21),
        Sekki("小暑", "暑さが始まる", 7, 7),
        Sekki("大暑", "一年でいちばん暑い頃", 7, 23),
        Sekki("立秋", "秋の気配", 8, 8),
        Sekki("処暑", "暑さが落ち着く", 8, 23),
        Sekki("白露", "朝露が白く光る", 9, 8),
        Sekki("秋分", "昼と夜が同じ長さ", 9, 23),
        Sekki("寒露", "露が冷たくなる", 10, 8),
        Sekki("霜降", "霜が降り始める", 10, 23),
        Sekki("立冬", "冬の気配", 11, 7),
        Sekki("小雪", "雪がちらつく頃", 11, 22),
        Sekki("大雪", "雪が本格的になる", 12, 7),
        Sekki("冬至", "一年でいちばん夜が長い", 12, 22),
    )

    private fun dayBefore(month: Int, day: Int): Pair<Int, Int> {
        if (day > 1) return month to (day - 1)
        val prevMonth = if (month == 1) 12 else month - 1
        val lastDay = when (prevMonth) {
            2 -> 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return prevMonth to lastDay
    }

    private data class Season(
        val key: String,
        val label: String,
        val startMonth: Int,
        val startDay: Int,
        val endMonth: Int,
        val endDay: Int,
        val backgroundArgb: Long,
        val motifColorArgb: Long,
        val motifPositions: List<Pair<Float, Float>>,
    )

    private val seasons = listOf(
        Season(
            "spring", "春", 3, 1, 5, 31, 0xFF33222E, 0xFFF9A8C9,
            listOf(0.18f to 0.62f, 0.30f to 0.70f, 0.85f to 0.66f),
        ),
        Season(
            "summer", "夏", 6, 1, 8, 31, 0xFF0E2A30, 0xFFD9F99D,
            listOf(0.22f to 0.66f, 0.72f to 0.72f, 0.60f to 0.60f),
        ),
        Season(
            "autumn", "秋", 9, 1, 11, 30, 0xFF241A2E, 0xFFF59E6C,
            listOf(0.20f to 0.64f, 0.82f to 0.60f, 0.66f to 0.70f),
        ),
        Season(
            "winter", "冬", 12, 1, 2, 28, 0xFF101A2C, 0xFFF8FAFC,
            listOf(0.25f to 0.60f, 0.75f to 0.66f, 0.50f to 0.72f),
        ),
    )

    private data class SpecialDay(
        val key: String,
        val month: Int,
        val day: Int,
        val text: String,
    )

    private val specialDays = listOf(
        SpecialDay("newyear", 1, 1, "あけましておめでとう"),
        SpecialDay("setsubun", 2, 3, "鬼は外、福は内"),
        SpecialDay("hinamatsuri", 3, 3, "ひな祭り"),
        SpecialDay("kodomo", 5, 5, "こどもの日"),
        SpecialDay("tanabata", 7, 7, "星に願いを"),
        SpecialDay("yama", 8, 11, "山の日"),
        SpecialDay("bunka", 11, 3, "文化の日"),
        SpecialDay("christmas", 12, 25, "メリークリスマス"),
        SpecialDay("omisoka", 12, 31, "今年もおつかれさま"),
    )

    /**
     * 暦めくり: a calendar-driven design. The season changes the background
     * and the drifting motifs, the current solar term (二十四節気) supplies
     * the headline word, and special days add one quiet line. Everything is
     * driven by date-range rules evaluated at draw time, so the face changes
     * on its own as the year turns — the reason comes from the calendar,
     * not from the app.
     */
    fun koyomi(): CanvasDesign {
        val elements = mutableListOf<CanvasElement>(
            ShapeElement(
                id = "koyomi_moon",
                xFraction = 0.78f,
                yFraction = 0.335f,
                widthFraction = 0.15f,
                heightFraction = 0.070f,
                shapeType = ShapeType.CIRCLE,
                fillColorArgb = 0xFFF3E5C0,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 0f,
                zIndex = 1,
            ),
            TextElement(
                id = "koyomi_date",
                xFraction = 0.5f,
                yFraction = 0.440f,
                text = "{date}",
                colorArgb = 0xFFD8DEE9,
                fontSizeSp = 15f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "koyomi_time",
                xFraction = 0.5f,
                yFraction = 0.520f,
                text = "{time}",
                colorArgb = 0xFFF8FAFC,
                fontSizeSp = 60f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            TextElement(
                id = "koyomi_sekki",
                xFraction = 0.5f,
                yFraction = 0.600f,
                text = "小雪",
                colorArgb = 0xFFF3E5C0,
                fontSizeSp = 21f,
                bold = true,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            TextElement(
                id = "koyomi_word",
                xFraction = 0.5f,
                yFraction = 0.648f,
                text = "雪がちらつく頃",
                colorArgb = 0xFFB9C4D6,
                fontSizeSp = 14f,
                bold = false,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            TextElement(
                id = "koyomi_special",
                xFraction = 0.5f,
                yFraction = 0.700f,
                text = "",
                colorArgb = 0xFFF9A8C9,
                fontSizeSp = 14f,
                bold = false,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            BatteryGaugeElement(
                id = "koyomi_battery",
                xFraction = 0.5f,
                yFraction = 0.790f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.36f,
                heightFraction = 0.018f,
                activeColorArgb = 0xFFC9B48A,
                trackColorArgb = 0x33FFFFFF,
                textColorArgb = 0xFFB9C4D6,
                showLabel = true,
                zIndex = 13,
            ),
        )
        seasons.forEach { season ->
            season.motifPositions.forEachIndexed { index, (x, y) ->
                elements.add(
                    ShapeElement(
                        id = "koyomi_motif_${season.key}_${index + 1}",
                        xFraction = x,
                        yFraction = y,
                        widthFraction = 0.055f,
                        heightFraction = 0.026f,
                        shapeType = ShapeType.CIRCLE,
                        fillColorArgb = season.motifColorArgb,
                        strokeColorArgb = 0x00000000,
                        strokeWidthDp = 0f,
                        cornerRadiusDp = 0f,
                        zIndex = 2,
                        visible = false,
                    ),
                )
            }
        }

        val rules = mutableListOf<ConditionalRule>()
        seasons.forEach { season ->
            val range = RuleCondition.DateRange(
                startMonth = season.startMonth,
                startDay = season.startDay,
                endMonth = season.endMonth,
                endDay = season.endDay,
            )
            rules.add(
                ConditionalRule(
                    id = "koyomi_bg_${season.key}",
                    name = "${season.label}の背景",
                    condition = range,
                    action = RuleAction.OverrideBackground(season.backgroundArgb),
                ),
            )
            season.motifPositions.indices.forEach { index ->
                rules.add(
                    ConditionalRule(
                        id = "koyomi_motif_${season.key}_${index + 1}",
                        name = "${season.label}のモチーフ",
                        condition = range,
                        action = RuleAction.ToggleElementVisibility(
                            elementId = "koyomi_motif_${season.key}_${index + 1}",
                            visible = true,
                        ),
                    ),
                )
            }
        }
        sekkiTable.forEachIndexed { index, sekki ->
            val next = sekkiTable[(index + 1) % sekkiTable.size]
            val (endMonth, endDay) = dayBefore(next.month, next.day)
            val range = RuleCondition.DateRange(
                startMonth = sekki.month,
                startDay = sekki.day,
                endMonth = endMonth,
                endDay = endDay,
            )
            rules.add(
                ConditionalRule(
                    id = "koyomi_sekki_name_$index",
                    name = "節気：${sekki.name}",
                    condition = range,
                    action = RuleAction.OverrideElementText("koyomi_sekki", sekki.name),
                ),
            )
            rules.add(
                ConditionalRule(
                    id = "koyomi_sekki_word_$index",
                    name = "節気の言葉：${sekki.name}",
                    condition = range,
                    action = RuleAction.OverrideElementText("koyomi_word", sekki.word),
                ),
            )
        }
        specialDays.forEach { day ->
            rules.add(
                ConditionalRule(
                    id = "koyomi_special_${day.key}",
                    name = "特別な日：${day.text}",
                    condition = RuleCondition.DateRange(day.month, day.day, day.month, day.day),
                    action = RuleAction.OverrideElementText("koyomi_special", day.text),
                ),
            )
        }

        return CanvasDesign(
            id = ID_KOYOMI,
            name = "暦めくり",
            backgroundColorArgb = 0xFF101A2C,
            secondaryBackgroundColorArgb = 0xFF101A2C,
            backgroundGradientEnabled = false,
            animationStyle = AnimationStyle.FLOATING_PARTICLES,
            elements = elements,
            rules = rules,
            updatedAtEpochMs = 1_700_000_006_000L,
        )
    }

    private data class WeatherFace(
        val kind: String,
        val label: String,
        val word: String,
        val line: String,
        val backgroundArgb: Long,
        val motifIds: List<String>,
    )

    /**
     * 空模様: the sky decides the face. The engine feeds the current weather
     * kind into the snapshot; rules swap the background, the weather word and
     * one quiet factual line, and show the matching motif (sun / cloud /
     * rain streaks / snow). Facts only, no exaggeration.
     */
    fun soramoyo(): CanvasDesign {
        val elements = mutableListOf<CanvasElement>(
            ShapeElement(
                id = "sora_sun",
                xFraction = 0.80f,
                yFraction = 0.335f,
                widthFraction = 0.15f,
                heightFraction = 0.070f,
                shapeType = ShapeType.CIRCLE,
                fillColorArgb = 0xFFFBBF24,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 0f,
                zIndex = 1,
                visible = false,
            ),
            ShapeElement(
                id = "sora_cloud_1",
                xFraction = 0.76f,
                yFraction = 0.330f,
                widthFraction = 0.17f,
                heightFraction = 0.055f,
                shapeType = ShapeType.CIRCLE,
                fillColorArgb = 0xFF94A3B8,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 0f,
                zIndex = 1,
                visible = false,
            ),
            ShapeElement(
                id = "sora_cloud_2",
                xFraction = 0.85f,
                yFraction = 0.352f,
                widthFraction = 0.11f,
                heightFraction = 0.040f,
                shapeType = ShapeType.CIRCLE,
                fillColorArgb = 0xFF7C8DA6,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 0f,
                zIndex = 1,
                visible = false,
            ),
            TextElement(
                id = "sora_date",
                xFraction = 0.5f,
                yFraction = 0.440f,
                text = "{date}",
                colorArgb = 0xFFCBD5E1,
                fontSizeSp = 15f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "sora_time",
                xFraction = 0.5f,
                yFraction = 0.520f,
                text = "{time}",
                colorArgb = 0xFFF8FAFC,
                fontSizeSp = 60f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            TextElement(
                id = "sora_word",
                xFraction = 0.5f,
                yFraction = 0.600f,
                text = "くもり",
                colorArgb = 0xFFF8FAFC,
                fontSizeSp = 22f,
                bold = true,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            TextElement(
                id = "sora_line",
                xFraction = 0.5f,
                yFraction = 0.648f,
                text = "空は落ち着いている",
                colorArgb = 0xFFB9C4D6,
                fontSizeSp = 14f,
                bold = false,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            TextElement(
                id = "sora_temp",
                xFraction = 0.5f,
                yFraction = 0.700f,
                text = "{weather_temp}",
                colorArgb = 0xFFCBD5E1,
                fontSizeSp = 16f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 12,
            ),
            BatteryGaugeElement(
                id = "sora_battery",
                xFraction = 0.5f,
                yFraction = 0.790f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.36f,
                heightFraction = 0.018f,
                activeColorArgb = 0xFF7DD3FC,
                trackColorArgb = 0x33FFFFFF,
                textColorArgb = 0xFFB9C4D6,
                showLabel = true,
                zIndex = 13,
            ),
        )
        // Rain streaks: thin tilted pills.
        val rainPositions = listOf(
            0.20f to 0.60f, 0.38f to 0.68f, 0.55f to 0.615f, 0.70f to 0.70f, 0.84f to 0.63f,
        )
        rainPositions.forEachIndexed { index, (x, y) ->
            elements.add(
                ShapeElement(
                    id = "sora_rain_${index + 1}",
                    xFraction = x,
                    yFraction = y,
                    widthFraction = 0.012f,
                    heightFraction = 0.050f,
                    shapeType = ShapeType.PILL,
                    fillColorArgb = 0xFF7DD3FC,
                    strokeColorArgb = 0x00000000,
                    strokeWidthDp = 0f,
                    cornerRadiusDp = 6f,
                    rotationDegrees = 15f,
                    zIndex = 2,
                    visible = false,
                ),
            )
        }
        // Snow dots.
        val snowPositions = listOf(
            0.22f to 0.615f, 0.42f to 0.685f, 0.60f to 0.60f, 0.74f to 0.665f, 0.86f to 0.715f,
        )
        snowPositions.forEachIndexed { index, (x, y) ->
            elements.add(
                ShapeElement(
                    id = "sora_snow_${index + 1}",
                    xFraction = x,
                    yFraction = y,
                    widthFraction = 0.030f,
                    heightFraction = 0.014f,
                    shapeType = ShapeType.CIRCLE,
                    fillColorArgb = 0xFFF8FAFC,
                    strokeColorArgb = 0x00000000,
                    strokeWidthDp = 0f,
                    cornerRadiusDp = 0f,
                    zIndex = 2,
                    visible = false,
                ),
            )
        }

        val faces = listOf(
            WeatherFace(
                DynamicDataSnapshot.WEATHER_CLEAR, "晴れ", "晴れ", "洗濯日和",
                0xFF1D3A52, listOf("sora_sun"),
            ),
            WeatherFace(
                DynamicDataSnapshot.WEATHER_CLOUDY, "くもり", "くもり", "空は落ち着いている",
                0xFF26313D, listOf("sora_cloud_1", "sora_cloud_2"),
            ),
            WeatherFace(
                DynamicDataSnapshot.WEATHER_RAIN, "雨", "雨", "傘を忘れずに",
                0xFF1B2735,
                listOf("sora_rain_1", "sora_rain_2", "sora_rain_3", "sora_rain_4", "sora_rain_5"),
            ),
            WeatherFace(
                DynamicDataSnapshot.WEATHER_SNOW, "雪", "雪", "足元に気をつけて",
                0xFF2A3242,
                listOf("sora_snow_1", "sora_snow_2", "sora_snow_3", "sora_snow_4", "sora_snow_5"),
            ),
        )
        val rules = mutableListOf<ConditionalRule>()
        faces.forEach { face ->
            val condition = RuleCondition.WeatherIs(face.kind)
            rules.add(
                ConditionalRule(
                    id = "sora_bg_${face.kind}",
                    name = "天気の背景：${face.label}",
                    condition = condition,
                    action = RuleAction.OverrideBackground(face.backgroundArgb),
                ),
            )
            rules.add(
                ConditionalRule(
                    id = "sora_word_${face.kind}",
                    name = "天気の言葉：${face.label}",
                    condition = condition,
                    action = RuleAction.OverrideElementText("sora_word", face.word),
                ),
            )
            rules.add(
                ConditionalRule(
                    id = "sora_line_${face.kind}",
                    name = "天気の一行：${face.label}",
                    condition = condition,
                    action = RuleAction.OverrideElementText("sora_line", face.line),
                ),
            )
            face.motifIds.forEach { motifId ->
                rules.add(
                    ConditionalRule(
                        id = "sora_show_$motifId",
                        name = "天気のモチーフ：${face.label}",
                        condition = condition,
                        action = RuleAction.ToggleElementVisibility(motifId, visible = true),
                    ),
                )
            }
        }

        return CanvasDesign(
            id = ID_SORAMOYO,
            name = "空模様",
            backgroundColorArgb = 0xFF26313D,
            secondaryBackgroundColorArgb = 0xFF26313D,
            backgroundGradientEnabled = false,
            animationStyle = AnimationStyle.NONE,
            elements = elements,
            rules = rules,
            updatedAtEpochMs = 1_700_000_007_000L,
        )
    }

    /**
     * 次の予定: the next appointment quietly owns the face. The event name
     * and a factual countdown come from the calendar through the snapshot;
     * as the start approaches the background warms slightly and a small
     * dot appears — no shouting, just the fact getting closer. With no
     * upcoming event the design stays calm.
     */
    fun nextEvent(): CanvasDesign {
        val elements = listOf<CanvasElement>(
            TextElement(
                id = "next_date",
                xFraction = 0.5f,
                yFraction = 0.420f,
                text = "{date}",
                colorArgb = 0xFFCBD5E1,
                fontSizeSp = 15f,
                bold = false,
                dynamicTemplate = true,
                zIndex = 10,
            ),
            TextElement(
                id = "next_time",
                xFraction = 0.5f,
                yFraction = 0.500f,
                text = "{time}",
                colorArgb = 0xFFF8FAFC,
                fontSizeSp = 60f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 11,
            ),
            TextElement(
                id = "next_label",
                xFraction = 0.5f,
                yFraction = 0.585f,
                text = "次の予定",
                colorArgb = 0xFF8FA3B8,
                fontSizeSp = 13f,
                bold = false,
                dynamicTemplate = false,
                zIndex = 12,
            ),
            TextElement(
                id = "next_event",
                xFraction = 0.5f,
                yFraction = 0.628f,
                text = "{next_event}",
                colorArgb = 0xFFF8FAFC,
                fontSizeSp = 19f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 12,
            ),
            TextElement(
                id = "next_countdown",
                xFraction = 0.5f,
                yFraction = 0.688f,
                text = "{countdown}",
                colorArgb = 0xFFFBBF24,
                fontSizeSp = 26f,
                bold = true,
                dynamicTemplate = true,
                zIndex = 12,
            ),
            ShapeElement(
                id = "next_dot",
                xFraction = 0.5f,
                yFraction = 0.745f,
                widthFraction = 0.035f,
                heightFraction = 0.016f,
                shapeType = ShapeType.CIRCLE,
                fillColorArgb = 0xFFF59E6C,
                strokeColorArgb = 0x00000000,
                strokeWidthDp = 0f,
                cornerRadiusDp = 0f,
                zIndex = 2,
                visible = false,
            ),
            BatteryGaugeElement(
                id = "next_battery",
                xFraction = 0.5f,
                yFraction = 0.795f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.36f,
                heightFraction = 0.018f,
                activeColorArgb = 0xFF7DD3FC,
                trackColorArgb = 0x33FFFFFF,
                textColorArgb = 0xFFB9C4D6,
                showLabel = true,
                zIndex = 13,
            ),
        )
        val rules = listOf(
            ConditionalRule(
                id = "next_bg_180",
                name = "予定まで3時間以内の背景",
                condition = RuleCondition.EventWithin(minutes = 180),
                action = RuleAction.OverrideBackground(0xFF2E2620),
            ),
            ConditionalRule(
                id = "next_dot_180",
                name = "予定が近い印",
                condition = RuleCondition.EventWithin(minutes = 180),
                action = RuleAction.ToggleElementVisibility("next_dot", visible = true),
            ),
            ConditionalRule(
                id = "next_bg_60",
                name = "予定まで1時間以内の背景",
                condition = RuleCondition.EventWithin(minutes = 60),
                action = RuleAction.OverrideBackground(0xFF38241C),
            ),
        )
        return CanvasDesign(
            id = ID_NEXT_EVENT,
            name = "次の予定",
            backgroundColorArgb = 0xFF1E2A32,
            secondaryBackgroundColorArgb = 0xFF1E2A32,
            backgroundGradientEnabled = false,
            animationStyle = AnimationStyle.NONE,
            elements = elements,
            rules = rules,
            updatedAtEpochMs = 1_700_000_008_000L,
        )
    }
}
