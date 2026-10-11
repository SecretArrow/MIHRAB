package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

/**
 * Palet ARGB mentah. Dipisah dari Compose agar rasio kontras (WCAG) dapat diuji di JVM
 * tanpa menjalankan kerangka Android — lihat ContrastTest.
 */
internal object Palette {
    /** Hijau zamrud — dipakai sebagai primary di tema terang (teks putih di atasnya). */
    val ZAMRUD = 0xFF0B7A55.toInt()

    /** Varian terang zamrud — primary tema gelap agar teks/ikon kontras di latar gelap. */
    val ZAMRUD_TERANG = 0xFF4FC79A.toInt()

    /** Teks di atas ZAMRUD_TERANG (tombol terisi tema gelap). */
    val ON_ZAMRUD_TERANG = 0xFF00382A.toInt()

    /** Latar tema terang. */
    val GADING = 0xFFFFFBF0.toInt()

    /** Emas gelap — secondary tema terang (dipakai sebagai warna teks Latin di latar terang). */
    val EMAS = 0xFF8A6B14.toInt()

    /** Emas terang — secondary tema gelap. */
    val EMAS_TERANG = 0xFFD8BC55.toInt()

    /** Latar tema gelap Material 3 (default darkColorScheme) — acuan uji kontras. */
    val DARK_BG = 0xFF1C1B1F.toInt()
}

private val Zamrud = Color(Palette.ZAMRUD)
private val Gading = Color(Palette.GADING)
private val Emas = Color(Palette.EMAS)

private val Light = lightColorScheme(primary = Zamrud, onPrimary = Color.White, secondary = Emas, background = Gading)

// Tema gelap memakai varian terang sebagai primary/secondary dan teks gelap di atas primary —
// bila tetap memakai Zamrud/emas gelap, teks di latar gelap gagal ambang kontras AA.
private val Dark = darkColorScheme(
    primary = Color(Palette.ZAMRUD_TERANG),
    onPrimary = Color(Palette.ON_ZAMRUD_TERANG),
    secondary = Color(Palette.EMAS_TERANG),
)

@Composable
fun IslamiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
