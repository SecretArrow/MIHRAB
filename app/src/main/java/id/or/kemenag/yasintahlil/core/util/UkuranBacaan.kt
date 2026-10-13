package id.or.kemenag.yasintahlil.core.util

/**
 * Ukuran tampilan bacaan: rentang yang sah **dan** nilai bawaannya, di satu tempat.
 *
 * Bawaannya dahulu ditulis ulang di tiap tempat yang membaca `Store.ARAB_SIZE` — enam layar memakai
 * `22f` dan lima memakai `24f`. Akibatnya, sebelum pengguna pernah menyentuh slidernya, bacaan yang
 * sama dirender 22 sp di layar Sholat tetapi 24 sp di layar Yasin, dan angka yang ditampilkan layar
 * Pengaturan (24) tidak cocok dengan yang benar-benar tampil di layar Sholat. Preferensinya sendiri
 * memang satu kunci; hanya bawaannya yang bercabang. Sekarang nilainya satu, dan uji menjaga agar
 * tiap bawaan tetap berada di dalam rentangnya.
 */
object UkuranBacaan {
    /** Rentang yang sah; dipakai bersama oleh sheet pengaturan bacaan dan layar Pengaturan. */
    val ARAB = 16f..40f
    val LATIN = 12f..24f
    val JARAK = 1.4f..2.6f

    /** Nilai bawaan selama pengguna belum pernah menulis preferensinya. */
    const val ARAB_DEFAULT = 24f
    const val LATIN_DEFAULT = 16f
    const val JARAK_DEFAULT = 1.9f

    /** Penambahan ukuran Arab saat bacaan dimaksudkan dibaca dari jauh, dan batas atasnya. */
    const val TAMBAHAN_BACA_JAUH = 8f
    const val MAKS_BACA_JAUH = 44f

    /**
     * Ukuran Arab saat bacaan dimaksudkan **dibaca dari jauh** — mode layar penuh, mode fokus, atau
     * mode jamaah: sedikit lebih besar agar terbaca, tetapi tetap dibatasi supaya tidak melewati
     * lebar kartu. Angka `+ 8` dan `44f` dahulu ditulis ulang di empat layar, tiga di antaranya
     * bahkan tanpa batas atas. Syaratnya diikutkan ke sini supaya tak ada layar yang menaikkan
     * ukuran tanpa pernah mengecilkannya lagi.
     *
     * **Kapan dipanggil.** Setiap layar baca yang punya salah satu mode itu wajib melewatkan ukuran
     * Arabnya ke fungsi ini. Dahulu hanya sebagian yang melakukannya: layar Ghaib, Jenazah, dan
     * Iqomah menaikkannya di layar penuh, Tahlil di mode jamaah — sedangkan mode fokus Yasin, layar
     * penuh Dzikir, dan layar penuh Tahlil sendiri **tidak**, sehingga mode yang sama menampilkan
     * teks lebih kecil di satu layar daripada di layar lain, tanpa satu pun pesan galat.
     * `UkuranBacaanModeBacaTest` menjaga agar setiap layar bermode begitu tetap memanggilnya.
     */
    fun arabBacaJauh(dasar: Float, aktif: Boolean): Float =
        if (!aktif) dasar else (dasar + TAMBAHAN_BACA_JAUH).coerceAtMost(MAKS_BACA_JAUH)
}
