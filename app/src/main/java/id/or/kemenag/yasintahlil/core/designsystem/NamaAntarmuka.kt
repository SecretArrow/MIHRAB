package id.or.kemenag.yasintahlil.core.designsystem

/**
 * Nama **kendali antarmuka** yang muncul di lebih dari satu tempat — bukan seluruh teks layar,
 * hanya kata yang dipakai bersama oleh beberapa layar atau oleh komponen bersama di `UiKit`.
 *
 * Tiga di antaranya sudah terlanjur ditulis berulang:
 *
 * - *"Pengaturan bacaan"* adalah judul sheet [BacaanSettingsSheet] **dan** label tombol yang
 *   membukanya di sepuluh tempat. Nama yang sama untuk hal yang sama, ditulis sebelas kali:
 *   mengganti nama sheet-nya meninggalkan sepuluh tombol yang menyebut nama lama — tombol tidak
 *   lagi menyebut apa yang dibukanya.
 * - *"Layar penuh"* adalah label chip di enam layar bacaan, dengan satu varian yang memang berbeda
 *   ([LAYAR_PENUH_IMAM] — pada Sholat Ghaib layar penuh berarti mode imam).
 * - *"Coba lagi"* ditulis dua kali **di dalam berkas yang sama**: sebagai bawaan `labelTombol` pada
 *   [EmptyState] dan sebagai literal tombol pada [ErrorState].
 *
 * Ketiganya kini satu tempat, sehingga nama kendali dan komponen yang memakainya tidak dapat
 * berbeda pendapat.
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
}
