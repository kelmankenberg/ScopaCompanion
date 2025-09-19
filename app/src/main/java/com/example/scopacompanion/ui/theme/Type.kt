package com.example.scopacompanion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.scopacompanion.ThemeSetting // Import ThemeSetting

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

// Primary Color Definitions
val PrimaryPurple = Color(0xFF6200EE)
val PrimaryRed = Color(0xFFC62828)
val PrimaryGreen = Color(0xFF2E7D32)
val PrimaryBlue = Color(0xFF1976D2)
val PrimaryDarkYellow = Color(0xFFFFA000)
val PrimaryDarkGray = Color(0xFF424242)

// Default theme colors (can be used as fallbacks or for components not affected by dynamic primary)
val DefaultScopaGreen = Color(0xFF2E7D32)
val DefaultGoldCoin = Color(0xFFFDD835)
val DefaultMaterialTeal = Color(0xFF03DAC6)
val DefaultLightOrange = Color(0xFFFFAB40)


fun AppDarkColorScheme(primaryColor: Color = PrimaryRed) = darkColorScheme(
    primary = primaryColor,
    secondary = DefaultScopaGreen,
    tertiary = DefaultGoldCoin,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White, // Assuming white is good contrast for all chosen primary colors
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE3E3E3),
    onSurface = Color(0xFFE3E3E3),
)

fun AppLightColorScheme(primaryColor: Color = PrimaryPurple) = lightColorScheme(
    primary = primaryColor,
    secondary = DefaultMaterialTeal,
    tertiary = DefaultLightOrange,
    background = Color(0xFFF7F7F7),
    surface = Color.White,
    onPrimary = Color.White, // Assuming white is good contrast for all chosen primary colors
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun ScopaCompanionTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    userSelectedPrimaryColor: Color? = null, // New parameter for dynamic primary color
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) {
        AppDarkColorScheme(primaryColor = userSelectedPrimaryColor ?: PrimaryRed)
    } else {
        AppLightColorScheme(primaryColor = userSelectedPrimaryColor ?: PrimaryPurple)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
