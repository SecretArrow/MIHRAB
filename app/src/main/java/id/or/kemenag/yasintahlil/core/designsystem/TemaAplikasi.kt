package id.or.kemenag.yasintahlil.core.designsystem

/**
 * Mode tema antarmuka: terang, gelap, atau mengikuti tema sistem.
 *
 * Nilai yang sah dan artinya dahulu ditulis di **tiga** berkas: `MainViewModel` (bawaan `"sistem"`),
 * `MainActivity` (pemetaan `"terang"`/`"gelap"` ke `dark: Boolean`), dan layar Pengaturan (daftar
 * chip `listOf("terang", "gelap", "sistem")`). Menambah satu mode berarti harus ingat menyunting
 * ketiganya, dan mode yang lolos dari pemetaan diam-diam jatuh ke "ikut sistem" — pilihan pengguna
 * tampak tersimpan tetapi tidak berpengaruh. Karena itu pemetaannya ikut ke sini, bukan hanya daftar
 * namanya.
 *
 * Sengaja murni (tanpa Compose) agar dapat diuji di JVM; tema sistem dioper sebagai [sistemGelap]
 * supaya aturannya tetap dapat diuji tanpa Android.
 */
object TemaAplikasi {
    /** Selalu terang, apa pun tema sistem. */
    const val TERANG = "terang"

    /** Selalu gelap, apa pun tema sistem. */
    const val GELAP = "gelap"

    /** Mengikuti tema sistem — bawaan. */
    const val SISTEM = "sistem"

    /** Seluruh pilihan yang sah, urut untuk ditampilkan. */
    val SEMUA: List<String> = listOf(TERANG, GELAP, SISTEM)

    /**
     * Apakah tema harus gelap, dengan [sistemGelap] sebagai tema sistem saat ini. Nilai yang tak
     * dikenal diperlakukan sebagai [SISTEM], sama seperti tema halaman yang jatuh ke `IKUT`.
     */
    fun gelap(v: String, sistemGelap: Boolean): Boolean = when (v) {
        TERANG -> false
        GELAP -> true
        else -> sistemGelap
    }
}
