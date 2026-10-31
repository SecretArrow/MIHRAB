package id.or.kemenag.yasintahlil.core.designsystem

/**
 * Nama **kendali antarmuka** yang muncul di lebih dari satu tempat — bukan seluruh teks layar,
 * hanya kata yang dipakai bersama oleh beberapa layar atau oleh komponen bersama di `UiKit`.
 *
 * Sembilan belas di antaranya sudah terlanjur ditulis berulang:
 *
 * - *"Pengaturan bacaan"* adalah judul bagian pengaturan bacaan di layar Pengaturan —
 *   satu-satunya tempatnya. Dahulu ia judul bottom sheet pengaturan bacaan **sekaligus** label
 *   tombol yang membukanya di sepuluh layar: nama yang sama untuk hal yang sama, ditulis sebelas
 *   kali, dan mengganti namanya meninggalkan sepuluh tombol yang menyebut nama lama.
 * - *"Layar penuh"* adalah label kendali di beberapa layar bacaan, dengan satu varian yang memang
 *   berbeda ([LAYAR_PENUH_IMAM] — pada Sholat Ghaib layar penuh berarti mode imam).
 * - *"Coba lagi"* ditulis dua kali **di dalam berkas yang sama**: sebagai bawaan `labelTombol` pada
 *   [EmptyState] dan sebagai literal tombol pada [ErrorState].
 * - Enam **preferensi tampilan bacaan** — tiga sakelar ([TAMPIL_ARAB], [TAMPIL_LATIN],
 *   [TAMPIL_TERJEMAHAN]) dan tiga pengukur ([UKURAN_ARAB], [UKURAN_LATIN], [JARAK_ANTARBARIS]) —
 *   kini dijual di satu tempat: layar Pengaturan. Dahulu dijual di dua tempat (bottom sheet
 *   per layar bacaan + layar Pengaturan) yang sama-sama menulis kunci
 *   `Store` yang **sama** (`SHOW_ARAB`, `SHOW_LATIN`, `SHOW_TRANS`, `ARAB_SIZE`, `LATIN_SIZE`,
 *   `LINE_HEIGHT`), jadi keduanya memang mengatur satu preferensi yang sama — dan sebelum ini
 *   masing-masing mengeja namanya sendiri. Akibatnya sunyi: mengganti nama satu sakelar di sheet
 *   meninggalkan layar Pengaturan menyebut preferensi yang sama dengan kata lama, dan pengguna
 *   mengira ada dua pengaturan. Persis yang dicegah komentar `JadwalHariIni.ASAR_SEMUA` di layar
 *   itu: *"dua nama untuk satu nilai membuat pengguna mengira ada dua pengaturan"*.
 * - Enam **kendali langkah dan tindakan** ada di dua layar atau lebih dengan arti yang sama:
 *   *"← Sebelumnya"* / *"Berikutnya →"* pada panduan sholat dan susunan Tahlil, *"Batalkan tanda"*
 *   pada rincian sholat dan Wudhu, *"Kosongkan"* pada penyaring Panduan Sholat dan Dzikir,
 *   *"Lanjutkan"* pada beranda dan daftar favorit, serta awalan *"Terakhir dibaca: …"* pada ketiga
 *   layar itu beserta Yasin. Tiap pasangan menamai satu tindakan yang sama; kata yang berbeda di
 *   salah satunya membuat pengguna mengira ada dua tombol berbeda untuk hal yang sama.
 * - Empat **kata tindakan baku** — *"Batal"*, *"Hapus"*, *"Reset"*, *"Buka"* — juga muncul di
 *   beberapa layar. Karena singkat dan umum, tiap tempat cenderung mengejanya sendiri; padahal
 *   *"Batal"* di dialog mana pun berarti menutup tanpa melakukan apa pun, dan *"Hapus"* di mana pun
 *   berarti menghapus yang tersimpan. Kata yang berbeda di salah satunya membuat tindakan yang sama
 *   tampak berbeda.
 *
 * Semuanya kini satu tempat, sehingga nama kendali dan komponen yang memakainya tidak dapat
 * berbeda pendapat.
 *
 * **Batasnya.** *"Terjemahan"* di sini adalah label **sakelar tampilan** — menyembunyikan atau
 * menampilkan terjemahan pada bacaan. `AtribusiTeks` memakai kata yang sama untuk hal yang
 * berbeda: label baris **asal-usul** ("Terjemahan: LPMQ Kemenag"). Keduanya sengaja tetap
 * terpisah, dan uji penjaga memakunya supaya yang satu tidak menghapus yang lain.
 */
object NamaAntarmuka {

    /**
     * Judul bagian pengaturan bacaan di layar Pengaturan — satu-satunya tempatnya. Dahulu ia
     * sekaligus label tombol pembuka sheet-nya; namanya tetap satu sejak itu.
     */
    const val PENGATURAN_BACAAN = "Pengaturan bacaan"

    /** Label chip yang menyembunyikan bar aplikasi agar bacaan memenuhi layar. */
    const val LAYAR_PENUH = "Layar penuh"

    /**
     * Varian [LAYAR_PENUH] pada Sholat Ghaib: layar penuh sekaligus menampilkan panduan dalam
     * mode imam. Berbeda karena yang dijanjikan tombolnya memang berbeda, bukan karena salinan.
     */
    const val LAYAR_PENUH_IMAM = "Layar penuh (imam)"

    /** Tombol mengulang proses yang gagal — dipakai [EmptyState] dan [ErrorState]. */
    const val COBA_LAGI = "Coba lagi"

    // ---- Preferensi tampilan bacaan ------------------------------------------------------------
    //
    // Enam nama di bawah ini adalah label untuk kunci `Store` yang sama — kini dijual di satu
    // tempat: layar Pengaturan. Nama kuncinya dicatat di komentar tiap
    // konstanta supaya hubungan "satu preferensi, satu nama" tetap terlihat dari sini.

    /** Sakelar menampilkan teks Arab (`Store.SHOW_ARAB`). */
    const val TAMPIL_ARAB = "Arab"

    /** Sakelar menampilkan transliterasi Latin (`Store.SHOW_LATIN`). */
    const val TAMPIL_LATIN = "Latin"

    /**
     * Sakelar menampilkan terjemahan (`Store.SHOW_TRANS`).
     *
     * Berbeda dari `AtribusiTeks` yang memakai kata ini untuk label **asal-usul** teks: yang di
     * sini menyembunyikan/menampilkan terjemahan pada bacaan.
     */
    const val TAMPIL_TERJEMAHAN = "Terjemahan"

    /** Pengukur besar huruf Arab (`Store.ARAB_SIZE`). */
    const val UKURAN_ARAB = "Ukuran Arab"

    /** Pengukur besar huruf Latin (`Store.LATIN_SIZE`). */
    const val UKURAN_LATIN = "Ukuran Latin"

    /** Pengukur jarak antarbaris bacaan (`Store.LINE_HEIGHT`). */
    const val JARAK_ANTARBARIS = "Jarak antarbaris"

    // ---- Kendali langkah dan tindakan ----------------------------------------------------------
    //
    // Empat nama di bawah ini muncul di **lebih dari satu layar** dengan arti yang sama:
    // membatalkan tanda progres, mengosongkan kotak penyaring, melanjutkan bacaan terakhir, dan
    // awalan kartu "terakhir dibaca". Mengganti kata di satu layar saja meninggalkan layar lain
    // menyebut tindakan yang sama dengan kata lain — dan pengguna mengira keduanya tindakan yang
    // berbeda.
    //
    // Dua nama langkah ("← Sebelumnya" / "Berikutnya →") dahulu juga hidup di sini untuk panduan
    // sholat dan susunan Tahlil. Keduanya dibubarkan bersama tombolnya: seluruh langkah dan
    // bagian kini mengalir bersambung (permintaan pengguna, 2026-10-15), sehingga tidak ada lagi
    // tindakan "melangkah mundur/maju satu layar" yang perlu dinamai.

    /** Tombol membatalkan tanda progres yang sudah dibuat. */
    const val BATALKAN_TANDA = "Batalkan tanda"

    /** Tombol mengosongkan kotak penyaring di halaman yang menyaring isinya sendiri. */
    const val KOSONGKAN = "Kosongkan"

    /** Tombol melanjutkan bacaan terakhir yang tersimpan. */
    const val LANJUTKAN = "Lanjutkan"

    /**
     * Awalan kartu "terakhir dibaca" (`"Terakhir dibaca: …"`) — dipakai beranda, daftar
     * favorit/riwayat, dan layar Yasin. Isi sesudahnya berbeda-beda (nama bacaan, atau nomor ayat),
     * tetapi awalannya satu.
     */
    const val TERAKHIR_DIBACA = "Terakhir dibaca"

    // ---- Kata tindakan baku --------------------------------------------------------------------
    //
    // Empat kata di bawah ini adalah **tindakan** yang sama di beberapa layar: membatalkan pilihan
    // pada dialog, menghapus sesuatu yang tersimpan, mengembalikan penghitung ke awal, dan membuka
    // layar asal sebuah bacaan. Karena singkat dan umum, tiap tempat cenderung mengejanya sendiri —
    // dan kata yang berbeda di salah satunya membuat tindakan yang sama tampak berbeda.

    /**
     * Tombol membatalkan pilihan pada dialog: menutupnya tanpa melakukan apa pun.
     *
     * Berbeda dari [BATALKAN_TANDA], yang membatalkan **tanda progres** yang sudah dibuat — bukan
     * menutup dialog. Keduanya sengaja tetap terpisah karena yang dijanjikan tombolnya berbeda.
     */
    const val BATAL = "Batal"

    /**
     * Tombol menghapus sesuatu yang tersimpan — jenis suara adzan, bacaan favorit, atau satu bagian
     * data. Di mana pun, kata ini berarti menghapus.
     */
    const val HAPUS = "Hapus"

    /** Tombol mengembalikan penghitung ke awal. */
    const val RESET = "Reset"

    /** Tombol membuka layar asal sebuah bacaan. */
    const val BUKA = "Buka"

    /**
     * Pintu ke layar keterangan (activity terpisah): label aksesibilitas ikon Info kecil dan
     * awalan judul layar keterangannya. Satu nama untuk satu tujuan — tombol yang menyebutnya
     * memang membuka keterangan, bukan hal lain.
     */
    const val KETERANGAN = "Keterangan"

    /** Tombol kembali dari layar keterangan ke layar yang membukanya. */
    const val KEMBALI = "Kembali"
}
