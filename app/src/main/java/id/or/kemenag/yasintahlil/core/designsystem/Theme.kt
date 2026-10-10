package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val Zamrud = Color(0xFF0B7A55)
private val ZamrudGelap = Color(0xFF063D2B)
private val Gading = Color(0xFFFFFBF0)
private val Emas = Color(0xFFC9A227)

private val Light = lightColorScheme(primary = Zamrud, onPrimary = Color.White, secondary = Emas, background = Gading)
private val Dark = darkColorScheme(primary = Zamrud, onPrimary = Color.White, secondary = Emas)

@Composable
fun IslamiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
