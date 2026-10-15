package id.or.kemenag.yasintahlil.core.data

/**
 * Nilai bawaan preferensi ([Store]) — di satu tempat.
 *
 * Dahulu setiap tempat yang membaca preferensi menuliskan bawaannya sendiri sebagai literal:
 * `store.get(Store.SHOW_ARAB, true)` di sebelas layar, `store.get(Store.NOTIF, true)` di penjadwal,
 * pekerja latar, beranda, dan Pengaturan, dan koreksi Hijriah dengan `-2`/`2` yang ditulis ulang di
 * dua tombol. Preferensinya memang satu kunci, tetapi bawaannya bercabang — dan begitu satu layar
 * mengubah bawaannya, layar lain diam-diam tidak ikut berubah, sehingga pengguna yang belum pernah
 * menyentuh pengaturannya melihat perilaku berbeda di tiap layar.
 *
 * Kerusakan yang sama pernah benar-benar terjadi pada ukuran bacaan: bawaannya ditulis `22f` di enam
 * layar dan `24f` di lima layar lain (lihat `UkuranBacaan`), sehingga bacaan yang sama dirender
 * berbeda sebelum pengguna menyentuh slidernya.
 *
 * Karena itu bawaan tidak lagi ditulis sebagai literal di tempat pemakaian; uji
 * `BawaanPreferensiTest` memindai seluruh `app/src/main` untuk memastikannya, dan `Store` sendiri
 * memakai nilai-nilai ini.
 *
 * Sengaja murni (tanpa Android) agar dapat diuji di JVM.
 */
object BawaanPreferensi {

    // --- Tampilan bacaan ----------------------------------------------------------------

    /** Tulisan Arab, Latin, dan terjemahan tampil selama pengguna belum memilih lain. */
    const val TAMPIL_ARAB = true
    const val TAMPIL_LATIN = true
    const val TAMPIL_TERJEMAHAN = true

    /**
     * Layar tidak dipaksa tetap menyala selama membaca (`Store.KEEP_ON`): bawaannya mati karena
     * memaksa layar menyala menghabiskan baterai tanpa diminta. Pengaturannya terpusat di tab
     * Pengaturan; dulu ia chip lokal di layar Yasin.
     */
    const val TETAP_MENYALA = false

    /**
     * Penghitung ulangan (tombol + dan − hitungan) di layar Tahlil dan Dzikir (`Store.SHOW_COUNTER`).
     * Bawaannya mati — opt-in, bukan dimatikan paksa: penghitung memang berguna saat menghitung
     * tasbih, tetapi ia mengisi layar dengan deretan tombol pada bacaan yang mayoritas dibaca
     * sekali. Yang menginginkannya menyalakannya sekali di tab Pengaturan; hitungan yang sudah
     * tersimpan tidak hilang saat penghitungnya disembunyikan.
     */
    const val TAMPIL_PENGHITUNG = false

    // --- Pengingat ----------------------------------------------------------------------

    /** Pengingat waktu sholat aktif selama pengguna belum mematikannya. */
    const val PENGINGAT_WAKTU = true

    /**
     * Pengingat doa setelah adzan dan pengingat waktu iqomah bersifat pilihan (opt-in): bawaannya
     * mati, karena keduanya bergantung pada kebiasaan masjid yang tidak dapat diasumsikan.
     */
    const val PENGINGAT_DOA_ADZAN = false
    const val PENGINGAT_IQOMAH = false

    /**
     * Pengingat imsak juga opt-in: imsak hanya bermakna bagi yang berpuasa, dan menitnya
     * (sebelum Subuh) disimpan terpisah di [MENIT_IMSAK].
     */
    const val PENGINGAT_IMSAK = false

    /** Menit sebelum Subuh yang dianggap imsak; rentang sahnya dijaga [RENTANG_MENIT_IMSAK]. */
    const val MENIT_IMSAK = 10

    /** Rentang menit imsak yang masuk akal, dipakai tombol − / + di Pengaturan. */
    val RENTANG_MENIT_IMSAK = 5..30

    /**
     * Menit imsak yang sah: nilai tersimpan di luar [RENTANG_MENIT_IMSAK] ditarik ke batas
     * terdekat, sehingga pengaturannya tidak pernah menuliskan angka yang tak masuk akal.
     */
    fun menitImsak(menit: Int): Int = menit.coerceIn(RENTANG_MENIT_IMSAK)

    /** Onboarding belum selesai bagi pengguna baru; "Mulai" menandainya sekali untuk selamanya. */
    const val ONBOARDING_SELESAI = false

    // --- Beranda ------------------------------------------------------------------------

    /** Kartu "Info Islami harian" tampil selama pengguna belum mematikannya. */
    const val KARTU_INFO = true

    /** Koreksi hari kalender Hijriah; nilai sahnya dijaga [RENTANG_KOREKSI_HIJRIAH]. */
    const val KOREKSI_HIJRIAH = 0

    /** Rentang koreksi Hijriah yang masuk akal, dipakai tombol − / + di Pengaturan. */
    val RENTANG_KOREKSI_HIJRIAH = -2..2

    // --- Lokasi & jadwal ----------------------------------------------------------------

    /**
     * Koordinat diambil dari GPS, dan jadwal ikut berubah saat pengguna berpindah tempat. Keduanya
     * mati secara bawaan: sebelum pengguna menentukan lokasinya sendiri, aplikasi memakai kota
     * bawaan — meminta GPS tanpa diminta akan mengejutkan.
     */
    const val LOKASI_DARI_GPS = false
    const val JADWAL_IKUT_LOKASI = false

    // --- Bacaan & berkas tersimpan ------------------------------------------------------

    /** Belum ada bacaan terakhir; kosong berarti kartu "Terakhir dibaca" tidak tampil. */
    const val TERAKHIR_DIBACA = ""

    /** Belum ada berkas suara adzan yang dipilih, dan belum ada daftar jenis suara tersimpan. */
    const val BERKAS_ADZAN = ""
    const val DAFTAR_SUARA_ADZAN = ""

    /** Belum ada susunan tahlil buatan pengguna (CSV id bagian) maupun susunan yang dipilih. */
    const val SUSUNAN_SAYA = ""
    const val SUSUNAN_TAHLIL_DIPILIH = ""

    /** Penanda ayat Yasin terakhir; `0` berarti belum ada. */
    const val PENANDA_YASIN = 0

    // --- Kunci warisan (legacy) ---------------------------------------------------------

    /**
     * Favorit, riwayat, dan posisi Tahlil dahulu disimpan di DataStore dan kini ada di Room; ketiga
     * kuncinya tinggal untuk memindahkan data lama (`MigrasiWarisan`). Bawaannya tetap di
     * sini agar pemindahnya tidak menuliskannya sendiri — dan agar "belum ada data lama" dapat
     * dibedakan dari "ada, tapi kosong".
     */
    val FAVORIT: Set<String> = emptySet()
    const val RIWAYAT = ""

    /** Posisi Tahlil warisan (`posisi:hitungan`); kosong berarti versi lama tak pernah menyimpannya. */
    const val PROGRES_TAHLIL = ""

    /**
     * Koreksi Hijriah yang sah: nilai tersimpan di luar [RENTANG_KOREKSI_HIJRIAH] ditarik ke batas
     * terdekat, sehingga tombol − / + tidak pernah menuliskan angka yang tak masuk akal — dan kedua
     * tombol itu tidak perlu mengingat batasnya masing-masing.
     */
    fun koreksiHijriah(hari: Int): Int = hari.coerceIn(RENTANG_KOREKSI_HIJRIAH)
}
