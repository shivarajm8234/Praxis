package ai.helply.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FullWhiteColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    secondary = AccentTeal,
    tertiary = PrimaryBlue,
    background = Color(0xFFF8FAFC), // Pure clean light slate background
    surface = Color(0xFFFFFFFF),    // Pure white cards & dialogs
    surfaceVariant = Color(0xFFF1F5F9), // Light grey input fields & chips
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A), // Dark slate text
    onSurface = Color(0xFF0F172A),    // Dark slate text on surface
    onSurfaceVariant = Color(0xFF64748B), // Subtitle text
    primaryContainer = NavActivePill, // Light purple/indigo pill background #EDE9FE
    onPrimaryContainer = NavActiveIcon // Active icon color #4F46E5
)

@Composable
fun HelplyTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = FullWhiteColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color(0xFFF8FAFC).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
