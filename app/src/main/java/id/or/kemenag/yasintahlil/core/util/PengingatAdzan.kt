package id.or.kemenag.yasintahlil.core.util

/**
 * Aturan murni untuk **preferensi pengingat adzan**: saklar per waktu dan panjang timer iqomah.
 *
 * Sama seperti [JadwalHariIni] menyatukan aturan hisab, berkas ini menyatukan aturan preferensi
 * yang sebelumnya disalin ke beberapa tempat:
 *
 *  - `Store.ADZAN_ON` adalah CSV lima angka yang diurai ulang di `PrayerScheduler` (pembaca) dan di
 *    layar Adzan (penulis). Bila keduanya tidak sepakat soal panjang atau padding, menyalakan satu
 *    waktu di layar dapat mematikan waktu lain di penjadwal.
 *  - Batas panjang timer iqomah (1–60 menit) dahulu ditulis di tiga tempat: dua tombol di layar
 *    Iqomah, penjadwal pengingat, dan pekerja latar. Bila salah satunya berbeda, layar menampilkan
 *    angka yang tidak pernah dipakai notifikasi.
 *  - Volume suara adzan bawaan (`0.8f`) ditulis di dua tempat: pekerja latar yang benar-benar
 *    memutar suaranya dan layar Adzan yang menampilkan slidernya.
 *  - Saklar bawaan `"1,1,1,1,1"` dan rentang jeda pengingat sebelum adzan (`0..30` menit) pun
 *    masing-masing ditulis di dua dan satu tempat, tanpa penjepitan di jalur baca.
 *  - Penjelasan pengingat "baca doa setelah adzan" dan jedanya (`5` menit) ditulis di layar Adzan
 *    **dan** layar Iqomah dengan kata-kata sendiri-sendiri, padahal keduanya mengatur saklar yang
 *    sama (`Store.DOA_ADZAN_REMINDER`).
 *  - Keterangan batas tanggung jawab timer iqomah — bahwa waktunya berbeda tiap masjid dan karena
 *    itu hanya perkiraan pribadi — ditulis di **empat** tempat dengan kata yang berbeda-beda: dua
 *    kali di layar Iqomah dan sekali di notifikasi yang berbunyi saat perkiraan waktunya habis,
 *    sehingga pengguna membaca keterangan yang tidak sama di layar dan di notifikasi untuk keadaan
 *    yang sama. Kini satu kalimat: [IQOMAH_PERKIRAAN].
 *
 * Tanpa Android, sehingga dapat diuji langsung di JVM.
 */
object PengingatAdzan {

    /** Banyak waktu beradzan — panjang CSV `Store.ADZAN_ON`. Sejajar dengan [JadwalHariIni.LIMA_WAKTU]. */
    val JUMLAH_WAKTU = JadwalHariIni.LIMA_WAKTU.size

    /**
     * "1,0,1,…" → lima saklar per waktu. Panjangnya selalu tepat [JUMLAH_WAKTU]: entri yang hilang
     * dianggap **menyala** (perilaku bawaan aplikasi) dan kelebihan dipotong, agar indeks tidak
     * bergeser bila CSV-nya cacat.
     */
    fun parseOn(csv: String): List<Boolean> =
        (csv.split(",").map { it.trim() == "1" } + List(JUMLAH_WAKTU) { true }).take(JUMLAH_WAKTU)

    /** Kebalikan [parseOn]: saklar → CSV yang dibaca penjadwal dan pekerja latar. */
    fun serializeOn(on: List<Boolean>): String = on.map { if (it) "1" else "0" }.joinToString(",")

    /**
     * Saklar bawaan: seluruh waktu **menyala** — aplikasi memang mengingatkan semua waktu. CSV
     * `"1,1,1,1,1"` dahulu ditulis langsung di `PrayerScheduler` dan layar Adzan; bila panjangnya
     * berbeda dari [JUMLAH_WAKTU], [parseOn] menganggap entri yang hilang menyala sehingga
     * perbedaannya tidak terlihat — tetapi CSV yang lebih panjang memotong waktu terakhir.
     */
    val ON_DEFAULT: String = serializeOn(List(JUMLAH_WAKTU) { true })

    /** Batas panjang timer iqomah, dalam menit. Nilai tersimpan selalu dijepit ke rentang ini. */
    const val IQOMAH_MIN_MENIT = 1
    const val IQOMAH_MAKS_MENIT = 60

    /** Panjang timer iqomah bawaan bila pengguna belum pernah mengaturnya. */
    const val IQOMAH_DEFAULT_MENIT = 10

    /** Jepit nilai tersimpan ke rentang yang sah, sehingga layar dan notifikasi memakai angka sama. */
    fun iqomahMenit(mentah: Int): Int = mentah.coerceIn(IQOMAH_MIN_MENIT, IQOMAH_MAKS_MENIT)

    /**
     * Batas tanggung jawab aplikasi atas timer iqomah — **satu** kalimat untuk semua tempat yang
     * menyebutnya: keterangan di bawah timer dan pada penjelasan saklarnya di layar Iqomah, serta
     * notifikasi yang berbunyi saat perkiraan waktunya habis.
     *
     * Dahulu keempat tempat itu menulis versinya sendiri-sendiri — *"Ini pengingat pribadi — waktu
     * iqomah tiap masjid berbeda"*, *"Waktu iqomah tiap masjid berbeda — ini pengingat pribadi"*,
     * dan *"…jadi ini perkiraan pribadi — bukan ketentuan syariat"* — sehingga pengguna membaca
     * keterangan yang berbeda di notifikasi dan di layar untuk keadaan yang sama. Yang paling
     * merugikan adalah salinan yang paling pendek: ia menyebut "pengingat pribadi" tanpa menyebut
     * bahwa waktu iqomah memang **berbeda tiap masjid**, padahal itulah sebabnya timer ini tidak
     * boleh dibaca sebagai jadwal masjid tempat pengguna sholat.
     */
    const val IQOMAH_PERKIRAAN =
        "Waktu iqomah tiap masjid berbeda, jadi ini perkiraan pribadi — bukan ketentuan syariat."

    /**
     * Batas jeda pengingat sebelum adzan, dalam menit. 0 berarti **tanpa** pengingat tambahan —
     * nilai itu sah dan memang bawaannya.
     */
    const val PRE_MIN_MIN_MENIT = 0
    const val PRE_MIN_MAKS_MENIT = 30

    /** Jeda pengingat sebelum adzan bawaan: 0, yaitu tepat pada waktu adzan. */
    const val PRE_MIN_DEFAULT = PRE_MIN_MIN_MENIT

    /**
     * Jepit jeda tersimpan ke rentang yang sah, sama seperti [iqomahMenit]. Rentangnya dahulu hanya
     * hidup sebagai `valueRange` slider di layar Adzan: nilai tersimpan di luar rentang itu (mis.
     * dari backup yang disunting) akan dihormati penjadwal apa adanya — pengingat berbunyi jauh
     * lebih awal daripada yang dapat dipilih di layar.
     */
    fun preMenit(mentah: Int): Int = mentah.coerceIn(PRE_MIN_MIN_MENIT, PRE_MIN_MAKS_MENIT)

    /**
     * Volume suara adzan bawaan, 0–1. Dibaca pekerja latar saat memutar suara dan layar Adzan saat
     * menampilkan slidernya — dua tempat yang sama-sama menuliskan `0.8f` sendiri-sendiri, sehingga
     * angka di layar bisa berbeda dari volume yang benar-benar berbunyi.
     */
    const val VOL_DEFAULT = 0.8f

    // ---- Pengingat "baca doa setelah adzan" -------------------------------------------------
    //
    // Saklar pengingat ini dapat diubah dari **dua** layar (Adzan dan Iqomah) yang menulis kunci
    // `Store.DOA_ADZAN_REMINDER` yang sama, dan keduanya menjelaskannya dengan kata-kata
    // masing-masing. Jeda `5` menit itu sendiri tinggal di `PrayerScheduler` — pekerja yang
    // benar-benar menjadwalkannya — sehingga penjelasan di layar menyebut angka yang tidak dapat
    // dibaca ulang dari tempat ia ditulis.

    /** Jeda pengingat "baca doa setelah adzan" dari waktu masuk, dalam menit. */
    const val DOA_DELAY_MIN = 5

    /**
     * Inti penjelasan pengingat itu, sama di layar Adzan dan Iqomah. Sengaja berupa potongan
     * kalimat (bukan kalimat penuh): tiap layar menempelkannya pada pembukanya sendiri
     * (*"Bila aktif: …"* / *"Bila diaktifkan: …"*) dan menyebutkan sendiri ke mana ketukannya
     * membuka layar — hal yang memang berbeda antarlayar.
     */
    const val DOA_SEBAB =
        "pengingat kedua $DOA_DELAY_MIN menit setelah waktu masuk, berisi ajakan membaca sholawat " +
            "dan doa setelah adzan"

    /**
     * Pengingat ini mengikuti saklar per waktu yang sama dengan pengingat adzan — satu kalimat,
     * karena keadaan yang dijelaskan sama. Dahulu layar Adzan menyebut "saklar per waktu **di atas**"
     * dan layar Iqomah "di menu Adzan"; keterangan tempat itu tidak menambah apa pun (saklarnya
     * terlihat di kedua layar) tetapi membuat dua kalimat yang seharusnya sama menjadi berbeda.
     */
    const val DOA_IKUT_SAKLAR =
        "Mengikuti saklar per waktu — waktu yang dimatikan tidak ikut mengingatkan."

    /** Batas tanggung jawab aplikasi: waktunya anjuran, bukan ketetapan syariat. */
    const val DOA_BUKAN_SYARIAT = "Waktunya pengingat pribadi, bukan ketentuan syariat."

    // ---- Nama saklar yang muncul di lebih dari satu layar -------------------------------------
    //
    // Saklar pengingat dapat dinyalakan/dimatikan dari beberapa layar, dan tiap layar menuliskan
    // namanya sendiri. Kalimat penjelasnya sudah lama disatukan di atas; **namanya** belum, dan
    // salah satunya sudah berkembang menjadi dua: saklar utama `Store.NOTIF` bernama "Pengingat
    // waktu sholat" di beranda tetapi "Pengingat ibadah" di Pengaturan. Pengguna yang mematikan
    // pengingat di beranda lalu membuka Pengaturan menemukan saklar yang sama dengan nama lain —
    // atau mengira keduanya pengaturan yang berbeda.

    /**
     * Nama saklar utama pengingat waktu sholat (`Store.NOTIF`) — dibaca beranda dan Pengaturan.
     *
     * "Waktu sholat" disebut eksplisit: saklar ini mematikan pengingat adzan, bukan seluruh
     * pengingat aplikasi.
     */
    const val NAMA_PENGINGAT_WAKTU = "Pengingat waktu sholat"

    /**
     * Nama saklar pengingat doa setelah adzan (`Store.DOA_ADZAN_REMINDER`) — dibaca layar Adzan dan
     * layar Iqomah, yang keduanya menuliskannya lengkap sendiri-sendiri.
     */
    const val NAMA_PENGINGAT_DOA = "Pengingat membaca doa setelah adzan"
}
