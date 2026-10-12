package id.or.kemenag.yasintahlil.navigation

object Routes {
    const val HOME = "home"
    const val IBADAH = "ibadah"
    const val ALAT = "alat"
    const val SETTINGS = "settings"
    const val YASIN = "yasin"
    const val TAHLIL = "tahlil"
    const val SHOLAT = "sholat"
    const val SHOLAT_DETAIL = "sholat_detail"
    const val WUDHU = "sholat/wudhu"
    const val GHAIB = "sholat/ghaib"
    const val JENAZAH = "sholat/jenazah"
    const val ADZAN = "adzan"
    const val DOA_ADZAN = "adzan/doa"
    const val IQOMAH = "iqomah"
    const val KIBLAT = "kiblat"
    const val JADWAL = "jadwal"
    const val DZIKIR = "dzikir"
    const val SEARCH = "search"
    const val FAVORIT = "favorit"
    const val KONTEN_UPDATE = "settings/konten"
    const val TENTANG = "settings/tentang"

    /**
     * Rute yang boleh dituju langsung dari luar layar (mis. ketukan notifikasi pengingat).
     *
     * [SHOLAT_DETAIL] sengaja tidak ada di sini: rutenya butuh argumen `{id}`, sehingga membuka
     * `"sholat_detail"` tanpa id akan gagal. Notifikasi yang menyebut rute di luar daftar ini
     * diabaikan (aplikasi tetap terbuka di beranda) alih-alih membuat aplikasi berhenti.
     */
    val LANGSUNG: Set<String> = setOf(
        HOME, IBADAH, ALAT, SETTINGS, YASIN, TAHLIL, SHOLAT, WUDHU, GHAIB, JENAZAH,
        ADZAN, DOA_ADZAN, IQOMAH, KIBLAT, JADWAL, DZIKIR, SEARCH, FAVORIT, KONTEN_UPDATE,
        TENTANG,
    )
}
