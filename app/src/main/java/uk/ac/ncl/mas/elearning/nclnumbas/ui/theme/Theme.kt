package uk.ac.ncl.mas.elearning.nclnumbas.ui.theme

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

private val DarkColorScheme = darkColorScheme(
        background = Color(0xFF1A2933),
        onBackground = Color.White,
        primary = Color(0xFF2f7dd4),
        onPrimary = Color.White,
        surfaceVariant = Color(0xFF213f52),
        onSurfaceVariant = Color(0xFFC7C9CA),
    )

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFa2d1f0),
    onBackground = Color.Black,
    primary = Color(0xFF0E66C7),
    onPrimary = Color.White,

    /* Other default colors to override
    surface = Color(0xFFFFFBFE),
    secondary = Color.Magenta,
    tertiary = Color.Yellow
    onSecondary = Color.White,
    onTertiary = Color.White,
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun NumbasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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