package com.roguehockey.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TabletopColors {
    val ChassisDark = Color(0xFF1E293B)
    val ChassisMid = Color(0xFF334155)
    val AluminumRim = Color(0xFFCBD5E1)
    val IceCenter = Color(0xFFF8FCFF)
    val IceEdge = Color(0xFFE3EFF7)
    val IceLineBlue = Color(0xFF38BDF8)
    val IceLineRed = Color(0xFFFB7185)
    val SurfaceCard = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF1F5F9)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)

    val Coral = Color(0xFFFF5A5F)
    val Amber = Color(0xFFF59E0B)
    val Emerald = Color(0xFF10B981)
    val Cyan = Color(0xFF06B6D4)
    val Sky = Color(0xFF0284C7)
    val Violet = Color(0xFF7C3AED)
    val Orange = Color(0xFFF97316)
    val GoldCoin = Color(0xFFEAB308)
}

private val LightTabletopScheme = lightColorScheme(
    primary = TabletopColors.Sky,
    onPrimary = Color.White,
    secondary = TabletopColors.Coral,
    onSecondary = Color.White,
    tertiary = TabletopColors.Violet,
    background = TabletopColors.ChassisDark,
    surface = TabletopColors.SurfaceCard,
    onBackground = Color.White,
    onSurface = TabletopColors.TextPrimary
)

@Composable
fun RogueHockeyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightTabletopScheme,
        content = content
    )
}
