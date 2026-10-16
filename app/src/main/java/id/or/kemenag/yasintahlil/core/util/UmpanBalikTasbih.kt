package id.or.kemenag.yasintahlil.core.util

/**
 * Kosakata bersama mode umpan balik ketukan Tasbih — satu tempat untuk kunci, daftar sah, dan
 * labelnya, pola yang sama dengan `TampilanKompas` dan `TemaHalaman`.
 *
 * Dahulu satu-satunya umpan balik ketukan adalah getar, ditulis tetap di layar Tasbih. Permintaan
 * pengguna menambah dua pilihan lagi — diam, dan bunyi beep dengan volume yang dapat diatur — dan
 * ketiganya harus terbaca sama di layar Pengaturan (rumah kendalinya, dijaga
 * `PengaturanTerpusatTest`), layar Tasbih (pelaksananya), layar Keterangan Tasbih (penjelasnya),
 * serta `Store` (penyimpannya). Tanpa kosakata bersama, empat tempat itu mengeja pilihannya
 * sendiri-sendiri dan salah satu pasti tertinggal saat mode berikutnya ditambah.
 *
 * Sengaja murni (tanpa Android) agar dapat diuji di JVM — `UmpanBalikTasbihTest`.
 */
object UmpanBalikTasbih {

    /** Tanpa getar dan tanpa bunyi — lingkaran cukup berganti warna saat target tercapai. */
    const val DIAM = "diam"

    /** Getar singkat tiap ketukan; perilaku lama sebelum opsi ini ada, jadi bawaannya. */
    const val GETAR = "getar"

    /** Bunyi beep tiap ketukan melalui speaker, volumenya mengikuti [Store.TASBIH_BEEP_VOL]. */
    const val BEEP = "beep"

    /** Seluruh mode yang sah, urut untuk ditampilkan sebagai chip di Pengaturan. */
    val SEMUA: List<String> = listOf(DIAM, GETAR, BEEP)

    /**
     * Mode bagi pengguna baru: getar — sama dengan perilaku sebelum pilihan ini diperkenalkan,
     * sehingga pembaruan aplikasi tidak diam-diam mengubah rasa ketukan yang sudah terbiasa.
     */
    const val BAWAAN = GETAR

    /** Volume beep bawaan, 0–1: cukup terdengar tanpa mengagetkan — senada volume adzan. */
    const val VOL_BAWAAN = 0.8f

    /** Batas volume beep: senyap sampai penuh. Satu-satunya tempat rentang ini dieja. */
    const val VOL_MIN = 0f
    const val VOL_MAKS = 1f

    /**
     * Jepit volume tersimpan ke rentang yang sah, sama seperti `PengingatAdzan.volume`: nilai di
     * luar rentang (mis. dari backup yang disunting) tidak boleh dihormati jalur baca apa adanya.
     */
    fun volume(nilai: Float): Float = nilai.coerceIn(VOL_MIN, VOL_MAKS)

    /**
     * Volume tersimpan (0–1) ke persen untuk pemain beep (ToneGenerator menerima 0–100).
     * Konversi unit ini sengaja satu tempat — layar Pengaturan yang menampilkan angkanya dan
     * layar Tasbih yang membunyikannya tidak boleh membulatkan secara berbeda.
     */
    fun persen(nilai: Float): Int = (volume(nilai) * 100).toInt().coerceIn(0, 100)

    /**
     * Nilai tersimpan yang tak dikenal — mis. teks lama dari backup versi lain — ditarik ke mode
     * bawaan, sehingga layar Tasbih tidak pernah membaca mode yang tak punya pelaksana.
     */
    fun benarkan(nilai: String): String = if (nilai in SEMUA) nilai else BAWAAN

    /** Label chip di Pengaturan; satu-satunya tempat ejaan label ini. */
    fun label(nilai: String): String = when (nilai) {
        DIAM -> "Diam"
        BEEP -> "Beep"
        else -> "Getar"
    }
}
