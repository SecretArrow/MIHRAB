package id.or.kemenag.yasintahlil.core.data

/**
 * Kategori sumber bacaan — memenuhi `plan.txt`: *"Bedakan bacaan yang bersumber dari Al-Qur'an,
 * hadis, dzikir umum, dan tradisi lokal."*
 *
 * Kuncinya sama persis dengan field `kategori` di `assets/content/*.json`; nilainya dijaga gate
 * `tools/validate_content.py` (nilai tak dikenal = gagal) dan `SumberKategoriTest`. Sebuah berkas
 * boleh menyatakan kategori sekali di tingkat berkas, dan entri boleh menimpanya (mis.
 * `sholat_bacaan.json` mayoritas hadis, tetapi lafaz niatnya tradisi).
 */
object SumberKategori {

    /** Ayat Al-Qur'an (MSI LPMQ Kemenag). */
    const val QURAN = "quran"

    /** Hadis/atsar dari kitab hadis (sunnah.com). */
    const val HADIS = "hadis"

    /** Dzikir & doa umum tanpa nisbah ayat/hadis tertentu. */
    const val DZIKIR = "dzikir"

    /** Rumusan yang lazim di Indonesia (mis. lafaz niat, susunan tahlil). */
    const val TRADISI = "tradisi"

    /** Semua kategori yang sah — harus selaras dengan `KATEGORI` di `tools/validate_content.py`. */
    val SEMUA = listOf(QURAN, HADIS, DZIKIR, TRADISI)

    /** Label tampilan untuk sebuah kunci kategori; kunci tak dikenal dikembalikan apa adanya. */
    fun label(kunci: String): String = when (kunci) {
        QURAN -> "Al-Qur'an"
        HADIS -> "Hadis"
        DZIKIR -> "Dzikir umum"
        TRADISI -> "Tradisi lokal"
        else -> kunci
    }

    /** True bila [kunci] salah satu kategori yang dikenal (kunci kosong bukan kategori). */
    fun dikenal(kunci: String): Boolean = kunci in SEMUA
}
