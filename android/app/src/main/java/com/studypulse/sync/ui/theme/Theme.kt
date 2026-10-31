package com.studypulse.sync.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val StudyPulseLightColors = lightColorScheme(
    primary = Blue40, onPrimary = Color.White, primaryContainer = Blue90, onPrimaryContainer = Blue20,
    secondary = Teal40, onSecondary = Color.White, secondaryContainer = Teal90, onSecondaryContainer = Teal20,
    tertiary = Amber40, onTertiary = Color.White, tertiaryContainer = Amber90, onTertiaryContainer = Amber20,
    error = Red40, onError = Color.White, errorContainer = Red90, onErrorContainer = Red20,
    background = Neutral98, onBackground = Neutral20,
    surface = Color.White, onSurface = Neutral20, surfaceVariant = Neutral96, onSurfaceVariant = Neutral20,
    outline = Neutral80
)

val StudyPulseDarkColors = darkColorScheme(
    primary = Blue80, onPrimary = Blue20, primaryContainer = Blue40, onPrimaryContainer = Blue90,
    secondary = Teal80, onSecondary = Teal20, secondaryContainer = Teal40, onSecondaryContainer = Teal90,
    tertiary = Amber80, onTertiary = Amber20, tertiaryContainer = Amber40, onTertiaryContainer = Amber90,
    error = Red80, onError = Red20, errorContainer = Red40, onErrorContainer = Red90,
    background = Neutral10, onBackground = Neutral90,
    surface = Neutral10, onSurface = Neutral90, surfaceVariant = Neutral20, onSurfaceVariant = Neutral80,
    outline = Neutral20
)

@Composable
fun StudyPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StudyPulseDarkColors
        else -> StudyPulseLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyPulseTypography,
        shapes = StudyPulseShapes,
        content = content
    )
}