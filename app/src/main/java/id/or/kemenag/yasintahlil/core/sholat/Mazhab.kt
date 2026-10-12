package id.or.kemenag.yasintahlil.core.sholat

/**
 * Mazhab rujukan panduan ibadah — `plan.txt` §"Tata Cara Sholat Langkah demi Langkah":
 * *"Pilihan mazhab atau rujukan"* dan *"Perbedaan mazhab dicatat, tidak dicampuradukkan"*.
 *
 * Nilai yang sah dan namanya dahulu ditulis di **tiga** tempat: `MAZHAB_OPTIONS` di layar panduan
 * sholat, sebuah `listOf(...)` di layar Pengaturan, dan sebuah rantai `when` untuk label yang
 * ditampilkan di kepala layar panduan. Dua daftar yang berbeda berarti satu layar menawarkan mazhab
 * yang tidak dikenal layar lain, dan label yang berbeda untuk kunci yang sama membuat kepala layar
 * menyebut mazhab yang tidak sedang dipilih.
 *
 * Sengaja murni (tanpa Android maupun Compose) agar daftar dan labelnya dapat diuji di JVM.
 */
object Mazhab {

    const val SYAFII = "syafii"
    const val HANAFI = "hanafi"
    const val MALIKI = "maliki"
    const val HAMBALI = "hambali"

    /**
     * Mazhab bawaan bila pengguna belum pernah memilihnya: Syafi'i — mazhab yang umum di Indonesia,
     * dan yang bacaannya (mis. qunut Subuh) paling banyak tersedia di konten aplikasi.
     */
    const val DEFAULT = SYAFII

    /** Seluruh pilihan sebagai pasangan kunci → nama tampilan, urut untuk ditampilkan. */
    val SEMUA: List<Pair<String, String>> = listOf(
        SYAFII to "Syafi'i",
        HANAFI to "Hanafi",
        MALIKI to "Maliki",
        HAMBALI to "Hambali",
    )

    /** True bila [kunci] salah satu mazhab yang dikenal. */
    fun sah(kunci: String): Boolean = SEMUA.any { it.first == kunci }

    /**
     * Nama tampilan untuk [kunci]. Nilai yang tak dikenal (mis. dari backup yang disunting) memakai
     * nama [DEFAULT], bukan kunci mentahnya — supaya kepala layar tidak menampilkan istilah asing.
     */
    fun label(kunci: String): String =
        SEMUA.firstOrNull { it.first == kunci }?.second ?: label(DEFAULT)
}
