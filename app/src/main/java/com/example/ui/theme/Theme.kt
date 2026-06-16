package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrandGreen,
    secondary = DarkLightSage,
    tertiary = DarkBrandGreen,
    background = DarkOffWhiteBg,
    surface = DarkWarmWhite,
    surfaceVariant = DarkOffWhiteBg,
    onPrimary = DarkOffWhiteBg,
    onSecondary = DarkCharcoalText,
    onBackground = DarkCharcoalText,
    onSurface = DarkCharcoalText,
    primaryContainer = DarkLightSage,
    onPrimaryContainer = DarkCharcoalText
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    secondary = LightSage,
    tertiary = BrandDarkGreen,
    background = OffWhiteBg,
    surface = WarmWhite,
    surfaceVariant = OffWhiteBg,
    onPrimary = WarmWhite,
    onSecondary = BrandDarkGreen,
    onBackground = CharcoalText,
    onSurface = CharcoalText,
    primaryContainer = LightSage,
    onPrimaryContainer = BrandDarkGreen
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false to prevent standard generic android material tokens override
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
