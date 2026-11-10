package com.nexora.x.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * NEXORA X Design System Color Tokens supporting both Dark and Light themes in Phase 2.
 */
object NexoraColors {
    // Dark Theme Surfaces
    val DeepNavyBackground = Color(0xFF060913)
    val BlackSurface = Color(0xFF0B0F1C)
    val ElevatedCardSurface = Color(0xFF11172A)
    val InteractiveSurface = Color(0xFF182038)
    val HairlineBorder = Color(0xFF1E293B)
    val SubtleHighlightBorder = Color(0xFF2E3C56)

    // Light Theme Surfaces
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightElevatedSurface = Color(0xFFF1F5F9)
    val LightBorder = Color(0xFFE2E8F0)
    val LightTextPrimary = Color(0xFF0F172A)
    val LightTextSecondary = Color(0xFF475569)
    val LightTextMuted = Color(0xFF64748B)

    // Brand Accents
    val ElectricPurple = Color(0xFF7C3AED)
    val ElectricPurpleBright = Color(0xFF8B5CF6)
    val ElectricPurpleSubtle = Color(0x267C3AED)

    val CyanHighlight = Color(0xFF22D3EE)
    val CyanHighlightLightMode = Color(0xFF0891B2)
    val CyanHighlightDim = Color(0x1F22D3EE)

    // Dark Theme Typography
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    // Semantic Status Colors
    val StatusDisconnected = Color(0xFF94A3B8)
    val StatusPendingAmber = Color(0xFFF59E0B)
    val StatusErrorCrimson = Color(0xFFEF4444)
    val StatusNominalEmerald = Color(0xFF10B981)
}
