package id.or.kemenag.yasintahlil.core.designsystem

/**
 * Nama **kendali antarmuka** yang muncul di lebih dari satu tempat — bukan seluruh teks layar,
 * hanya kata yang dipakai bersama oleh beberapa layar atau oleh komponen bersama di `UiKit`.
 *
 * Sembilan di antaranya sudah terlanjur ditulis berulang:
 *
 * - *"Pengaturan bacaan"* adalah judul sheet [BacaanSettingsSheet] **dan** label tombol yang
 *   membukanya di sepuluh tempat. Nama yang sama untuk hal yang sama, ditulis sebelas kali:
 *   mengganti nama sheet-nya meninggalkan sepuluh tombol yang menyebut nama lama — tombol tidak
 *   lagi menyebut apa yang dibukanya.
 * - *"Layar penuh"* adalah label chip di enam layar bacaan, dengan satu varian yang memang berbeda
 *   ([LAYAR_PENUH_IMAM] — pada Sholat Ghaib layar penuh berarti mode imam).
 * - *"Coba lagi"* ditulis dua kali **di dalam berkas yang sama**: sebagai bawaan `labelTombol` pada
 *   [EmptyState] dan sebagai literal tombol pada [ErrorState].
 * - Enam **preferensi tampilan bacaan** — tiga sakelar ([TAMPIL_ARAB], [TAMPIL_LATIN],
 *   [TAMPIL_TERJEMAHAN]) dan tiga pengukur ([UKURAN_ARAB], [UKURAN_LATIN], [JARAK_ANTARBARIS]) —
 *   ada di dua tempat: sheet [BacaanSettingsSheet] dan layar Pengaturan. Keduanya menulis kunci
 *   `Store` yang **sama** (`SHOW_ARAB`, `SHOW_LATIN`, `SHOW_TRANS`, `ARAB_SIZE`, `LATIN_SIZE`,
 *   `LINE_HEIGHT`), jadi keduanya memang mengatur satu preferensi yang sama — dan sebelum ini
 *   masing-masing mengeja namanya sendiri. Akibatnya sunyi: mengganti nama satu sakelar di sheet
 *   meninggalkan layar Pengaturan menyebut preferensi yang sama dengan kata lama, dan pengguna
 *   mengira ada dua pengaturan. Persis yang dicegah komentar `JadwalHariIni.ASAR_SEMUA` di layar
 *   itu: *"dua nama untuk satu nilai membuat pengguna mengira ada dua pengaturan"*.
 *
 * Kesembilannya kini satu tempat, sehingga nama kendali dan komponen yang memakainya tidak dapat
 * berbeda pendapat.
 *
 * **Batasnya.** *"Terjemahan"* di sini adalah label **sakelar tampilan** — menyembunyikan atau
 * menampilkan terjemahan pada bacaan. `AtribusiTeks` memakai kata yang sama untuk hal yang
 * berbeda: label baris **asal-usul** ("Terjemahan: LPMQ Kemenag"). Keduanya sengaja tetap
 * terpisah, dan uji penjaga memakunya supaya yang satu tidak menghapus yang lain.
 */
object NamaAntarmuka {

    /**
     * Judul sheet pengaturan bacaan dan label tombol yang membukanya. Satu nama, karena keduanya
     * menunjuk hal yang sama: tombol harus menyebut apa yang akan terbuka.
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
    // Enam nama di bawah ini adalah label untuk kunci `Store` yang sama di dua tempat: sheet
    // [BacaanSettingsSheet] dan layar Pengaturan. Nama kuncinya dicatat di komentar tiap
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
}
