package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

    /** Kertas gading untuk "tema halaman" — latar baca krem, lebih lembut dari latar aplikasi. */
    val KERTAS_KREM = 0xFFFBF2DF.toInt()

    /** Tinta di atas [KERTAS_KREM]. */
    val TINTA_KREM = 0xFF2E2416.toInt()

    /** Emas di atas [KERTAS_KREM]; lebih gelap dari [EMAS] karena krem lebih gelap dari [GADING]. */
    val EMAS_KERTAS = 0xFF7A5E10.toInt()

    /** Kertas gelap untuk "tema halaman" — nyaris hitam, bukan abu kebiruan. */
    val KERTAS_GELAP = 0xFF15130F.toInt()

    /** Tinta di atas [KERTAS_GELAP]. */
    val TINTA_GELAP = 0xFFEDE6D6.toInt()
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

/**
 * Sediakan tema halaman bacaan ([TemaHalaman]) ke seluruh isi aplikasi. Dipanggil sekali di akar
 * agar kartu bacaan di layar mana pun menghormati pilihan yang sama tanpa pengkabelan per layar.
 * Nilai tak dikenal jatuh ke [TemaHalaman.IKUT] supaya preferensi yang rusak tidak membuat
 * halaman tak terbaca.
 */
@Composable
fun ProvideTemaHalaman(tema: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTemaHalaman provides if (TemaHalaman.sah(tema)) tema else TemaHalaman.IKUT,
        content = content,
    )
}

/**
 * Skema warna halaman bacaan untuk [tema], atau `null` bila halaman mengikuti tema aplikasi.
 *
 * Yang diganti hanya warna permukaan kartu (`surfaceVariant`/`onSurfaceVariant`, warna bawaan
 * `Card` Material 3) dan aksen `primary`/`secondary`, sehingga tipografi, bentuk, dan seluruh
 * tata letak tetap sama — hanya "kertas" yang berubah. Pasangan warna diuji rasio kontrasnya
 * di `ContrastTest`.
 */
@Composable
fun skemaHalaman(tema: String): ColorScheme? = when (tema) {
    TemaHalaman.KREM -> MaterialTheme.colorScheme.copy(
        surface = Color(Palette.KERTAS_KREM),
        onSurface = Color(Palette.TINTA_KREM),
        surfaceVariant = Color(Palette.KERTAS_KREM),
        onSurfaceVariant = Color(Palette.TINTA_KREM),
        primary = Zamrud,
        secondary = Color(Palette.EMAS_KERTAS),
    )
    TemaHalaman.GELAP -> MaterialTheme.colorScheme.copy(
        surface = Color(Palette.KERTAS_GELAP),
        onSurface = Color(Palette.TINTA_GELAP),
        surfaceVariant = Color(Palette.KERTAS_GELAP),
        onSurfaceVariant = Color(Palette.TINTA_GELAP),
        primary = Color(Palette.ZAMRUD_TERANG),
        secondary = Color(Palette.EMAS_TERANG),
    )
    else -> null
}
