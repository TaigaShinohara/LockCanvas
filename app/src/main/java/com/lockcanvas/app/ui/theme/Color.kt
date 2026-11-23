package com.lockcanvas.app.ui.theme

import androidx.compose.ui.graphics.Color

// Dark-first minimal modern palette
val ObsidianBackground = Color(0xFF0A0D14)
val ObsidianSurface = Color(0xFF111622)
val ObsidianSurfaceVariant = Color(0xFF1A2233)
val ObsidianSurfaceHigh = Color(0xFF232D42)
val ObsidianOutline = Color(0xFF334155)
val ObsidianOutlineSubtle = Color(0xFF1E293B)

val SkyAccent = Color(0xFF64D2FF)
val OnSkyAccent = Color(0xFF002433)
val SkyContainer = Color(0xFF0B354A)
val OnSkyContainer = Color(0xFFC8EEFF)

val LavenderSecondary = Color(0xFFA5B4FC)
val OnLavenderSecondary = Color(0xFF181E42)
val LavenderContainer = Color(0xFF242B57)
val OnLavenderContainer = Color(0xFFE0E7FF)

val EmeraldTertiary = Color(0xFF34D399)
val OnEmeraldTertiary = Color(0xFF00291B)

val CoralError = Color(0xFFF87171)
val OnCoralError = Color(0xFF450A0A)
val CoralErrorContainer = Color(0xFF5C1515)
val OnCoralErrorContainer = Color(0xFFFEE2E2)

val SlateTextPrimary = Color(0xFFF8FAFC)
val SlateTextSecondary = Color(0xFF94A3B8)

// Light mode high-contrast palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F7)
val LightSurfaceHigh = Color(0xFFE2E8F0)
val LightOutline = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightPrimary = Color(0xFF0284C7)
val LightPrimaryContainer = Color(0xFFE0F2FE)
val LightOnPrimaryContainer = Color(0xFF0C4A6E)

object EditorColorSwatches {
    val foregroundColors: List<Long> = listOf(
        0xFFF8FAFC, // Pure White
        0xFFCBD5E1, // Cool Silver
        0xFF64D2FF, // Sky Cyan
        0xFF00F5D4, // Neon Mint
        0xFF34D399, // Emerald
        0xFFA5B4FC, // Soft Indigo
        0xFFC084FC, // Orchid Purple
        0xFFF472B6, // Rose Pink
        0xFFFBBF24, // Amber Gold
        0xFFF87171, // Coral Red
        0xFF0F172A, // Deep Ink
    )

    val backgroundColors: List<Long> = listOf(
        0xFF0B1020, // Midnight Navy
        0xFF070812, // Deep Obsidian
        0xFF0D1412, // Forest Dark
        0xFF19243E, // Slate Blue
        0xFF131032, // Cosmic Indigo
        0xFF2B0E17, // Crimson Night
        0xFF1A2A26, // Pine Mist
        0xFF1E1B2E, // Twilight Plum
        0xFF0F172A, // Slate 900
        0xFF18181B, // Zinc Carbon
        0xFFF1F5F9, // Soft Frost
    )

    val shapeFillColors: List<Long> = listOf(
        0x22FFFFFF, // Frosted White
        0x1600F5D4, // Mint Glass
        0x22A855F7, // Purple Glass
        0x2664D2FF, // Sky Glass
        0x26F87171, // Coral Glass
        0x33000000, // Smoked Dark
        0xFF1E293B, // Solid Slate
        0xFF0EA5E9, // Solid Sky
        0xFF10B981, // Solid Emerald
        0x00000000, // Transparent
    )
}
