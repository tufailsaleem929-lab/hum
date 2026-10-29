package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand accents
val NoteFlowCyan = Color(0xFF06B6D4)
val NoteFlowCyanBright = Color(0xFF38BDF8)
val NoteFlowViolet = Color(0xFF8B5CF6)
val NoteFlowVioletBright = Color(0xFFA78BFA)
val NoteFlowRose = Color(0xFFF43F5E)
val NoteFlowAmber = Color(0xFFF59E0B)
val NoteFlowEmerald = Color(0xFF10B981)

// Dark Theme Dashboard Backgrounds & Surfaces
val BackgroundDark = Color(0xFF090D16)
val SurfaceDark = Color(0xFF111827)
val SurfaceElevatedDark = Color(0xFF1E293B)
val SurfaceBorderDark = Color(0xFF334155)

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextTertiaryDark = Color(0xFF64748B)

// Light Theme Palette
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceElevatedLight = Color(0xFFF1F5F9)
val SurfaceBorderLight = Color(0xFFE2E8F0)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextTertiaryLight = Color(0xFF94A3B8)

// Vibrant Note Card Themes
data class NoteColorTheme(
    val id: String,
    val name: String,
    val bgDarkHex: String,
    val borderHex: String,
    val accentHex: String,
    val bgLightHex: String
)

val NOTE_COLOR_PALETTES = listOf(
    NoteColorTheme("default", "Obsidian", "#1E293B", "#475569", "#94A3B8", "#FFFFFF"),
    NoteColorTheme("violet", "Violet Dream", "#271E47", "#8B5CF6", "#C4B5FD", "#F5F3FF"),
    NoteColorTheme("ocean", "Electric Cyan", "#0E2A3E", "#06B6D4", "#67E8F9", "#ECFEFF"),
    NoteColorTheme("emerald", "Deep Forest", "#0D3327", "#10B981", "#6EE7B7", "#ECFDF5"),
    NoteColorTheme("amber", "Warm Honey", "#362208", "#F59E0B", "#FCD34D", "#FFFBEB"),
    NoteColorTheme("crimson", "Neon Rose", "#38121F", "#F43F5E", "#FDA4AF", "#FFF1F2"),
    NoteColorTheme("sunset", "Cosmic Sunset", "#341631", "#EC4899", "#F472B6", "#FDF2F8"),
    NoteColorTheme("indigo", "Royal Indigo", "#1B224E", "#6366F1", "#A5B4FC", "#EEF2FF")
)
