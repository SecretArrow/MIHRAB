package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Palet ARGB mentah. Dipisah dari Compose agar rasio kontras (WCAG) dapat diuji di JVM
 * tanpa menjalankan kerangka Android — lihat ContrastTest.
 *
 * Selain warna inti yang diuji kontrasnya, palet ini kini memuat **tonal layer** lengkap
 * (wadah mint, wadah emas, pasir, arang hijau) supaya seluruh komponen Material 3 — kartu,
 * chip, wadah ikon, bilah navigasi — diisi dari satu sumber kebenaran yang sama, bukan
 * campuran warna bawaan pabrik dan warna kustom.
 */
internal object Palette {
    /** Hijau zamrud — dipakai sebagai primary di tema terang (teks putih di atasnya). */
    val ZAMRUD = 0xFF0B7A55.toInt()

    /** Zamrud pekat — ujung bawah [GradienZamrud]; putih tetap lulus ambang kontras di atasnya. */
    val ZAMRUD_PEKAT = 0xFF06402E.toInt()

    /** Varian terang zamrud — primary tema gelap agar teks/ikon kontras di latar gelap. */
    val ZAMRUD_TERANG = 0xFF4FC79A.toInt()

    /** Teks di atas ZAMRUD_TERANG (tombol terisi tema gelap) dan di atas wadah mint. */
    val ON_ZAMRUD_TERANG = 0xFF00382A.toInt()

    /** Wadah mint — primaryContainer tema terang: kepala beranda dan wadah ikon. */
    val MINT = 0xFFC3F0DC.toInt()

    /** Latar tema terang. */
    val GADING = 0xFFFFFBF0.toInt()

    /** Permukaan kartu tema terang — sedikit lebih putih dari [GADING] agar kartu terangkat. */
    val PUTIH_SUSU = 0xFFFFFDF6.toInt()

    /** Tinta utama teks di tema terang. */
    val TINTA = 0xFF1D1B15.toInt()

    /** Pasir hangat — surfaceVariant terang: kertas bacaan Arab dan wadah sekunder. */
    val PASIR = 0xFFF0EADA.toInt()

    /** Teks di atas [PASIR]. */
    val ON_PASIR = 0xFF48443C.toInt()

    /** Emas gelap — secondary tema terang (dipakai sebagai warna teks Latin di latar terang). */
    val EMAS = 0xFF8A6B14.toInt()

    /** Wadah emas muda — secondaryContainer terang: lencana jumlah ulangan dan aksen lembut. */
    val EMAS_MUDA = 0xFFF5EBC8.toInt()

    /** Teks di atas [EMAS_MUDA]. */
    val ON_EMAS_MUDA = 0xFF3F3210.toInt()

    /** Emas terang — secondary tema gelap. */
    val EMAS_TERANG = 0xFFD8BC55.toInt()

    /** Wadah emas gelap — secondaryContainer tema gelap. */
    val EMAS_GELAP = 0xFF57470F.toInt()

    /** Teks di atas [EMAS_GELAP]. */
    val ON_EMAS_GELAP = 0xFFF5EBC8.toInt()

    /** Hijau lumut — tertiary tema terang, aksen pendamai di antara zamrud dan emas. */
    val LUMUT = 0xFF2F6457.toInt()

    /** Wadah lumut muda — tertiaryContainer terang. */
    val LUMUT_MUDA = 0xFFD0EAE1.toInt()

    /** Teks di atas [LUMUT_MUDA]. */
    val ON_LUMUT_MUDA = 0xFF05291F.toInt()

    /** Lumut terang — tertiary tema gelap. */
    val LUMUT_TERANG = 0xFFA5D9C9.toInt()

    /** Wadah lumut gelap — tertiaryContainer tema gelap. */
    val LUMUT_GELAP = 0xFF1E4F44.toInt()

    /** Teks di atas [LUMUT_GELAP]. */
    val ON_LUMUT_GELAP = 0xFFBFF0E0.toInt()

    /** Latar tema gelap — hitam kehijauan, bukan abu kebiruan pabrik: identitas zamrud terbawa. */
    val DARK_BG = 0xFF0E1613.toInt()

    /** Permukaan kartu tema gelap — selangkah lebih terang dari [DARK_BG]. */
    val PERMUKAAN_GELAP = 0xFF121B16.toInt()

    /** Putih hangat — teks utama tema gelap. */
    val PUTIH_HANGAT = 0xFFE1E5E0.toInt()

    /** Arang hijau — surfaceVariant gelap: kertas bacaan Arab dan wadah sekunder. */
    val ARANG_HIJAU = 0xFF17231D.toInt()

    /** Teks di atas [ARANG_HIJAU]. */
    val ON_ARANG_HIJAU = 0xFFC4CEC7.toInt()

    /** Wadah zamrud lembut — primaryContainer tema gelap. */
    val ZAMRUD_LEMBUT = 0xFF0A5C42.toInt()

    /** Teks di atas [ZAMRUD_LEMBUT]. */
    val ON_ZAMRUD_LEMBUT = 0xFFACF3D7.toInt()

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

/**
 * Tema terang: gading sebagai latar, putih susu untuk kartu, zamrud sebagai warna tindakan,
 * mint sebagai wadah lembut, dan emas sebagai aksen. Seluruh pasangan teks/latar yang benar-benar
 * dipakai diuji kontrasnya di `ContrastTest`.
 */
private val Terang = lightColorScheme(
    primary = Zamrud,
    onPrimary = Color.White,
    primaryContainer = Color(Palette.MINT),
    onPrimaryContainer = Color(Palette.ON_ZAMRUD_TERANG),
    secondary = Emas,
    onSecondary = Color.White,
    secondaryContainer = Color(Palette.EMAS_MUDA),
    onSecondaryContainer = Color(Palette.ON_EMAS_MUDA),
    tertiary = Color(Palette.LUMUT),
    onTertiary = Color.White,
    tertiaryContainer = Color(Palette.LUMUT_MUDA),
    onTertiaryContainer = Color(Palette.ON_LUMUT_MUDA),
    background = Gading,
    onBackground = Color(Palette.TINTA),
    surface = Color(Palette.PUTIH_SUSU),
    onSurface = Color(Palette.TINTA),
    surfaceVariant = Color(Palette.PASIR),
    onSurfaceVariant = Color(Palette.ON_PASIR),
    inverseSurface = Color(0xFF31302A),
    inverseOnSurface = Color(0xFFF3F0E7),
    inversePrimary = Color(Palette.ZAMRUD_TERANG),
    outline = Color(0xFF79746A),
    outlineVariant = Color(0xFFC9C4B7),
)

// Tema gelap memakai varian terang sebagai primary/secondary dan teks gelap di atas primary —
// bila tetap memakai Zamrud/emas gelap, teks di latar gelap gagal ambang kontras AA. Latarnya
// hitam kehijauan ([Palette.DARK_BG]) supaya identitas zamrud terbawa juga di mode gelap.
private val Gelap = darkColorScheme(
    primary = Color(Palette.ZAMRUD_TERANG),
    onPrimary = Color(Palette.ON_ZAMRUD_TERANG),
    primaryContainer = Color(Palette.ZAMRUD_LEMBUT),
    onPrimaryContainer = Color(Palette.ON_ZAMRUD_LEMBUT),
    secondary = Color(Palette.EMAS_TERANG),
    onSecondary = Color(0xFF3F2E04),
    secondaryContainer = Color(Palette.EMAS_GELAP),
    onSecondaryContainer = Color(Palette.ON_EMAS_GELAP),
    tertiary = Color(Palette.LUMUT_TERANG),
    onTertiary = Color(0xFF03372C),
    tertiaryContainer = Color(Palette.LUMUT_GELAP),
    onTertiaryContainer = Color(Palette.ON_LUMUT_GELAP),
    background = Color(Palette.DARK_BG),
    onBackground = Color(Palette.PUTIH_HANGAT),
    surface = Color(Palette.PERMUKAAN_GELAP),
    onSurface = Color(Palette.PUTIH_HANGAT),
    surfaceVariant = Color(Palette.ARANG_HIJAU),
    onSurfaceVariant = Color(Palette.ON_ARANG_HIJAU),
    inverseSurface = Color(Palette.PUTIH_HANGAT),
    inverseOnSurface = Color(0xFF2F312B),
    inversePrimary = Zamrud,
    outline = Color(0xFF8D988F),
    outlineVariant = Color(0xFF424D45),
)

/**
 * Skala huruf aplikasi: turunan Material 3 dengan bobot lebih tegas pada judul (w600) dan
 * jarak antarbaris lebih longgar pada badan teks (26 sp untuk 16 sp) — nyaman dibaca lama untuk
 * bacaan dan terjemahan tanpa memperbesar huruf terlebih dahulu.
 */
private val Tipografi = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp, lineHeight = 64.sp,
        fontWeight = FontWeight.W400, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp, lineHeight = 52.sp,
        fontWeight = FontWeight.W400, letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.W600,
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.W600,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.W600,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.W600,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.W600,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.W600, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.W600, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.W600, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.W600, letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp, lineHeight = 15.sp,
        fontWeight = FontWeight.W600, letterSpacing = 0.4.sp,
    ),
)

/**
 * Bentuk sudut aplikasi: kelengkungan besar (24–32 dp) untuk kartu dan lembar, lebih kecil
 * (8–16 dp) untuk kontrol rapat — proporsi modern yang tetap tenang, tidak membulat penuh.
 */
private val Bentuk = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Gradien zamrud untuk kartu waktu sholat berikutnya di beranda: identitas warna yang sama di
 * tema terang maupun gelap. Teks di atasnya putih — kontras terhadap kedua ujung gradien ini
 * (ZAMRUD dan ZAMRUD_PEKAT) sama-sama di atas ambang AA karena keduanya sekeluarga nilainya
 * dengan warna yang sudah lolos uji `ContrastTest` untuk teks putih.
 */
internal val GradienZamrud = Brush.verticalGradient(
    listOf(Color(Palette.ZAMRUD), Color(Palette.ZAMRUD_PEKAT))
)

@Composable
fun IslamiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Gelap else Terang,
        typography = Tipografi,
        shapes = Bentuk,
        content = content,
    )
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
