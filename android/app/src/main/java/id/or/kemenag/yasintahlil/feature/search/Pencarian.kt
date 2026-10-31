package id.or.kemenag.yasintahlil.feature.search

/**
 * Logika pencarian terpadu (`plan.txt`: *"Implementasikan pencarian terpadu untuk: Nama surat,
 * Nomor ayat, Nama doa, Bacaan sholat, Kata dalam terjemahan, Bagian Tahlil, Panduan ibadah"*).
 *
 * Sengaja dipisahkan dari layar dan dari Android: [SearchDoc] adalah dokumen yang **sudah** dibaca
 * dari konten, dan [Pencarian.cari] murni menyaringnya. Dengan begitu aturan pencocokan dapat diuji
 * sebagai unit test biasa, sementara layar hanya bertugas menyusun indeks dari `ContentRepo`.
 */
data class SearchDoc(
    val id: String,
    val judul: String,
    /** Semua teks yang boleh dicocokkan: judul, terjemahan, transliterasi, dan teks Arab. */
    val teks: List<String>,
    val rute: String,
    /**
     * Nomor ayat bila dokumen ini sebuah ayat — dicocokkan **persis**, bukan sebagai substring,
     * supaya mencari "1" tidak ikut memunculkan ayat 12, 13, dan seterusnya.
     */
    val nomor: Int? = null,
)

/** Satu baris hasil pencarian yang siap ditampilkan layar. */
data class Hit(val id: String, val judul: String, val cuplikan: String, val rute: String)

object Pencarian {

    /** Batas jumlah hasil agar daftar tetap ringan di perangkat kecil. */
    const val BATAS = 60

    /** Panjang cuplikan pada kartu hasil. */
    private const val PANJANG_CUPLIKAN = 120

    /**
     * Saring [dokumen] dengan kata kunci [kata]. Kata kosong berarti **tidak ada** hasil — bukan
     * seluruh konten — sehingga layar dapat menampilkan petunjuk awal alih-alih daftar panjang.
     * Urutan hasil mengikuti urutan [dokumen] agar stabil antarpencarian.
     */
    fun cari(dokumen: List<SearchDoc>, kata: String, batas: Int = BATAS): List<Hit> {
        val kunci = kata.trim()
        if (kunci.isBlank()) return emptyList()
        return dokumen.asSequence()
            .filter { it.cocok(kunci) }
            .take(batas.coerceAtLeast(1))
            .map { Hit(it.id, it.judul, it.cuplikan(kunci), it.rute) }
            .toList()
    }

    private fun SearchDoc.cocok(kunci: String): Boolean =
        nomor?.toString() == kunci || teks.any { it.contains(kunci, ignoreCase = true) }

    /**
     * Cuplikan diambil dari bagian yang benar-benar memuat kata kunci — bukan selalu awal teks —
     * supaya pengguna melihat alasan hasilnya muncul. Spasi/baris baru dirapikan agar tidak ada
     * baris kosong yang memakan ruang kartu.
     */
    private fun SearchDoc.cuplikan(kunci: String): String {
        val sumber = teks.firstOrNull { it.contains(kunci, ignoreCase = true) } ?: judul
        val bersih = sumber.replace(Regex("\\s+"), " ").trim()
        return if (bersih.length <= PANJANG_CUPLIKAN) bersih
        else bersih.take(PANJANG_CUPLIKAN - 1).trimEnd() + "…"
    }
}
