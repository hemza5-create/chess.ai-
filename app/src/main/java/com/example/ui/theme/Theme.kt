package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ChessVisionColorScheme = darkColorScheme(
    primary = BrandLime,
    onPrimary = OnBrandDark,
    primaryContainer = ColorTokens.LimeContainer,
    onPrimaryContainer = BrandLime,
    secondary = BrandGold,
    onSecondary = OnBrandDark,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = BrandGold,
    tertiary = SkyVision,
    onTertiary = OnBrandDark,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = DeepInputSurface,
    surfaceContainerLow = HeaderSurface,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = ElevatedSurface,
    outline = BorderMuted,
    outlineVariant = BorderSubtle,
    error = RoseBlunder,
    onError = OnBrandDark
)

private object ColorTokens {
    val LimeContainer = androidx.compose.ui.graphics.Color(0x26CCEB0D)
}

val ChessVisionShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ChessVisionTheme(
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChessVisionColorScheme,
        typography = buildAppTypography(isArabic),
        shapes = ChessVisionShapes,
        content = content
    )
}
