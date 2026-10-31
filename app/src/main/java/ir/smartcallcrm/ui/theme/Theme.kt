package ir.smartcallcrm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val Teal700 = Color(0xFF0D6E6E)
private val Teal500 = Color(0xFF138A8A)
private val Teal200 = Color(0xFF5FD0D0)
private val Amber = Color(0xFFE8A13A)

private val LightColors = lightColorScheme(
    primary = Teal700,
    secondary = Teal500,
    tertiary = Amber,
    primaryContainer = Teal200
)

private val DarkColors = darkColorScheme(
    primary = Teal200,
    secondary = Teal500,
    tertiary = Amber,
    primaryContainer = Teal700
)

/** App theme. Forces RTL for the whole tree, as the UI is Persian. */
@Composable
fun SmartCallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}
