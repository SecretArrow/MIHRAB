package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.runtime.compositionLocalOf

/**
 * Tema halaman bacaan (`plan.txt`: *"Pengaturan jarak antarbaris dan tema halaman"*).
 *
 * Halaman bacaan dapat memakai kertas gading atau halaman gelap terlepas dari tema antarmuka,
 * supaya nyaman dibaca lama tanpa harus mengubah seluruh aplikasi. Pilihan ini murni soal
 * tampilan: tidak menyentuh teks bacaan, urutannya, maupun sumbernya.
 *
 * Sengaja murni (tanpa Compose) agar pilihan yang sah dan namanya dapat diuji di JVM; pemetaan
 * ke warna ada di `skemaHalaman` (Theme.kt) dan diuji kontrasnya oleh `ContrastTest`.
 */
object TemaHalaman {
    /** Mengikuti tema aplikasi (terang/gelap) — bawaan. */
    const val IKUT = "ikut"

    /** Kertas gading: latar krem dengan tinta cokelat gelap. */
    const val KREM = "krem"

    /** Halaman gelap: latar hampir hitam dengan tinta terang. */
    const val GELAP = "gelap"

    /** Seluruh pilihan yang sah, urut untuk ditampilkan. */
    val SEMUA: List<String> = listOf(IKUT, KREM, GELAP)

    /** Apakah [v] pilihan yang dikenal — nilai tak dikenal diperlakukan sebagai [IKUT]. */
    fun sah(v: String): Boolean = v in SEMUA

    /** Nama pilihan untuk ditampilkan ke pengguna. */
    fun label(v: String): String = when (v) {
        KREM -> "Krem"
        GELAP -> "Gelap"
        else -> "Ikut tema"
    }
}

/**
 * Tema halaman bacaan yang sedang aktif. Disediakan sekali di akar aplikasi ([ProvideTemaHalaman])
 * dari preferensi pengguna, sehingga setiap kartu bacaan menghormatinya tanpa perlu dioper
 * satu per satu dari tiap layar.
 */
val LocalTemaHalaman = compositionLocalOf { TemaHalaman.IKUT }
