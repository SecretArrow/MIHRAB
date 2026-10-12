package id.or.kemenag.yasintahlil.core.sholat

/**
 * Kosakata **id bacaan sholat** pada bundel konten: `sholat_bacaan.json` (sholat wajib & sunnah)
 * dan `sholat_mayit.json` (doa sholat jenazah/ghaib).
 *
 * Id ini adalah kunci yang menghubungkan bundel konten dengan layar yang menampilkannya. Sebelum
 * berkas ini ada, id yang sama dieja sendiri-sendiri di **empat** tempat — peta posisi tubuh
 * (`Postures.byId`, 14 id), daftar langkah mode ringkas (`SholatDetailScreen`, 8 id), dan daftar
 * langkah takbir layar Sholat Ghaib serta Sholat Jenazah (masing-masing 5 id). Keempatnya tidak
 * pernah dibandingkan satu sama lain, sehingga salah eja pada salah satunya tidak berbunyi apa pun:
 *
 *  - `cari(id)` mengembalikan `null`, lalu kartu bacaan langkah itu **hilang** sementara judul
 *    bernomor di atasnya tetap tampil — pengguna melihat "Takbir 3 → Doa untuk mayit" tanpa isi,
 *    dan menyangka doanya memang tidak ada;
 *  - `it.id in listOf(…)` pada mode ringkas sekadar tidak cocok, sehingga satu langkah pokok
 *    diam-diam terlewat dari panduan yang justru dipakai pemula;
 *  - `Postures.of(id)` mengembalikan `null`, sehingga ilustrasi posisi tubuhnya hilang tanpa
 *    keterangan.
 *
 * Ketiganya kegagalan yang sama: satu ejaan yang berbeda antara penulis dan pembaca, tanpa galat.
 * Karena itu seluruh id tinggal di sini, dan **layar sholat tidak mengeja id apa pun** — uji
 * `BacaanSholatTest` memindai berkas-berkas itu untuk memastikannya.
 *
 * [NIAT] dan [TAKBIR] kebetulan sama bunyinya dengan kunci JSON pada `ContentRepo` dan dengan jenis
 * id tersimpan [id.or.kemenag.yasintahlil.core.util.JenisBacaan.NIAT]. Ketiganya hal yang berbeda —
 * yang di sini adalah id entri bacaan pada bundel — sehingga uji penjaga memindai paket layar
 * sholat saja, bukan seluruh kode.
 *
 * Murni tanpa Android, sehingga dapat diuji langsung di JVM.
 */
object BacaanSholat {

    // ---- Bacaan sholat wajib & sunnah (`sholat_bacaan.json`) ----------------------------------

    const val NIAT = "niat"
    const val TAKBIR = "takbir"
    const val IFTITAH = "iftitah"
    const val FATIHAH = "fatihah"
    const val SURAT_SETELAH_FATIHAH = "surat_setelah_fatihah"
    const val RUKUK = "rukuk"
    const val ITIDAL = "itidal"
    const val QUNUT = "qunut"
    const val SUJUD = "sujud"
    const val DUDUK = "duduk"
    const val TASYAHUD_AWAL = "tasyahud_awal"
    const val TASYAHUD_AKHIR = "tasyahud_akhir"
    const val SHOLAWAT_IBRAHIMIYAH = "sholawat_ibrahimiyah"
    const val SALAM = "salam"

    // ---- Bacaan sholat mayit (`sholat_mayit.json`) --------------------------------------------

    const val DOA_MAYIT = "doa_mayit"
    const val DOA_MAYIT_TAMBAHAN = "doa_mayit_tambahan"

    /**
     * Seluruh bacaan `sholat_bacaan.json`, **urut seperti di bundel**. Urutannya sendiri adalah
     * aturan: langkah dibaca berurutan lewat tombol "Berikutnya", jadi daftar ini juga yang
     * menentukan langkah mana yang muncul lebih dahulu.
     */
    val SEMUA: List<String> = listOf(
        NIAT, TAKBIR, IFTITAH, FATIHAH, SURAT_SETELAH_FATIHAH, RUKUK, ITIDAL, QUNUT,
        SUJUD, DUDUK, TASYAHUD_AWAL, TASYAHUD_AKHIR, SHOLAWAT_IBRAHIMIYAH, SALAM,
    )

    /** Bacaan tambahan untuk sholat mayit; berkasnya berbeda dari [SEMUA], jadi dipisah. */
    val MAYIT: List<String> = listOf(DOA_MAYIT, DOA_MAYIT_TAMBAHAN)

    /**
     * Langkah pokok untuk **mode ringkas**, urut seperti di [SEMUA].
     *
     * Dipilih agar sah untuk sholat dua rakaat sekalipun: tasyahud awal dan qunut sengaja tidak
     * ikut karena keduanya bersyarat rakaat atau mazhab. I'tidal, duduk antara dua sujud, iftitah,
     * dan sholawat Ibrahimiyah juga tidak ikut — ringkasan ini memang bukan panduan lengkap,
     * melainkan kerangka yang harus tetap benar untuk sholat terpendek.
     */
    val RINGKAS: List<String> = listOf(
        NIAT, TAKBIR, FATIHAH, SURAT_SETELAH_FATIHAH, RUKUK, SUJUD, TASYAHUD_AKHIR, SALAM,
    )

    /**
     * Bacaan tiap takbir sholat jenazah dan sholat ghaib — empat takbir, lalu salam — **urut**.
     *
     * Kedua sholat itu berbeda pada label dan pada varian niatnya, tetapi bacaan yang dibaca tiap
     * takbirnya sama; itulah yang disatukan di sini. Sebelumnya kelima id ini diketik ulang di
     * kedua layar, sehingga satu salah eja cukup untuk menghilangkan satu takbir dari salah
     * satunya saja.
     */
    val TAKBIR_MAYIT: List<String> = listOf(
        FATIHAH, SHOLAWAT_IBRAHIMIYAH, DOA_MAYIT, DOA_MAYIT_TAMBAHAN, SALAM,
    )
}
