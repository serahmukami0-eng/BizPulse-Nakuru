package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleSoft,
    onPrimaryContainer = BrandPurpleDark,
    secondary = BrandPurple,
    onSecondary = Color.White,
    secondaryContainer = BrandTealLight,
    onSecondaryContainer = BrandTealDark,
    tertiary = DemoAmber,
    onTertiary = Color.White,
    tertiaryContainer = DemoAmberLight,
    onTertiaryContainer = DemoAmberDark,
    background = SurfaceOffWhite,
    onBackground = InkDark,
    surface = CardWhite,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFEEF2F8),
    onSurfaceVariant = InkMuted,
    outline = CardBorderLight,
    error = AlertCoral,
    onError = Color.White,
    errorContainer = AlertCoralLight,
    onErrorContainer = AlertCoral
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurpleBright,
    onPrimary = BrandNavyDeep,
    primaryContainer = BrandNavyMid,
    onPrimaryContainer = Color(0xFFE4DDFE),
    secondary = BrandTealBright,
    onSecondary = BrandNavyDeep,
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = BrandPurpleSoft,
    tertiary = DemoAmber,
    onTertiary = DarkBg,
    tertiaryContainer = Color(0xFF452A08),
    onTertiaryContainer = DemoAmberLight,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = Color(0xFFF87171),
    onError = DarkBg,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

val ProjectXShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ProjectXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ProjectXTypography,
        shapes = ProjectXShapes,
        content = content
    )
}
