package id.or.kemenag.yasintahlil.core.legal

/** Satu blok pada dokumen legal yang dibundel (kebijakan privasi, izin, lisensi). */
sealed interface Blok {
    /** Judul bagian; [level] 1 = judul dokumen, 2 = bagian, 3+ = subbagian. */
    data class Judul(val level: Int, val teks: String) : Blok

    /** Satu butir daftar, sudah disatukan dengan baris lanjutannya. */
    data class Butir(val teks: String) : Blok

    /** Satu paragraf, sudah disatukan dengan baris lanjutannya. */
    data class Paragraf(val teks: String) : Blok

    /** Tabel: [judul] adalah baris kepala, [baris] isinya (masing-masing sepanjang [judul]). */
    data class Tabel(val judul: List<String>, val baris: List<List<String>>) : Blok
}

/**
 * Pengurai Markdown seadanya untuk dokumen legal di `assets/legal/`.
 *
 * Dokumen itu **tidak disalin ulang ke dalam kode**: berkas di `docs/` dan `LICENSE` disalin apa
 * adanya ke assets dan dijaga tetap identik oleh uji, sehingga kebijakan privasi yang dibaca
 * pengguna tidak mungkin menyimpang dari yang didokumentasikan di repositori. Yang ditambahkan di
 * sini hanyalah penampilan: judul, butir, paragraf, dan tabel dikenali agar dapat dirender
 * mengikuti tema aplikasi, dan penanda Markdown dibuang dari teks.
 *
 * Baris yang tidak dipisahkan baris kosong dianggap satu blok: dokumen ditulis dengan pembungkus
 * baris, jadi satu butir atau satu paragraf sering menempati beberapa baris. Tanpa penyatuan itu
 * setiap baris pembungkus akan tampil sebagai paragraf sendiri dan kalimatnya terputus-putus.
 *
 * Sengaja **tidak** mendukung gambar, kutipan, atau blok kode: dokumen yang dipakai hanya memuat
 * judul, butir, paragraf, dan satu tabel. Fitur yang tidak dipakai tidak perlu ditulis, apalagi
 * diuji.
 */
object MarkdownRingkas {

    /** `[label](tujuan)` → `label`: tujuan relatif tidak dapat dibuka di dalam aplikasi. */
    private val tautan = Regex("""\[([^\]]*)]\([^)]*\)""")

    /** `**tebal**` dan `*miring*` → teksnya saja. */
    private val penekanan = Regex("""\*{1,3}([^*]+)\*{1,3}""")

    /** Baris pemisah kepala tabel, mis. `|----|----|`. */
    private val pemisahTabel = Regex(""":?-{2,}:?""")

    /** Buang penanda Markdown sebaris; sisakan teksnya. */
    fun rapi(teks: String): String = teks.trim()
        .replace(tautan, "$1")
        .replace(penekanan, "$1")
        .replace("`", "")
        .trim()

    fun blok(markdown: String): List<Blok> {
        val out = mutableListOf<Blok>()
        for (p in potongan(markdown)) {
            val pertama = p.first().trimStart()
            when {
                p.all { it.trimStart().startsWith("|") } -> tabel(p)?.let { out += it }

                pertama.startsWith("#") -> {
                    val level = pertama.takeWhile { it == '#' }.length
                    out += Blok.Judul(level, rapi(pertama.drop(level)))
                }

                pertama.startsWith("- ") -> out += butir(p)

                else -> out += Blok.Paragraf(rapi(p.joinToString(" ") { it.trim() }))
            }
        }
        return out
    }

    /**
     * Butir-butir dalam satu potongan: baris berawalan `- ` membuka butir baru, baris lain menjadi
     * lanjutannya. Butir berurutan tanpa baris kosong tetap terpisah — kalau tidak, tiga izin di
     * dokumen akan menyatu menjadi satu butir panjang dan salah satunya tampak hilang.
     */
    private fun butir(p: List<String>): List<Blok> {
        val out = mutableListOf<Blok>()
        var kini = ""
        for (b in p) {
            val t = b.trim()
            if (t.startsWith("- ")) {
                if (kini.isNotEmpty()) out += Blok.Butir(rapi(kini))
                kini = t.removePrefix("- ")
            } else {
                kini = if (kini.isEmpty()) t else "$kini $t"
            }
        }
        if (kini.isNotEmpty()) out += Blok.Butir(rapi(kini))
        return out
    }

    /** Kelompokkan baris menjadi blok; baris kosong memisahkan blok. */
    private fun potongan(markdown: String): List<List<String>> {
        val hasil = mutableListOf<List<String>>()
        val kini = mutableListOf<String>()
        for (b in markdown.lines()) {
            if (b.isBlank()) {
                if (kini.isNotEmpty()) {
                    hasil += kini.toList()
                    kini.clear()
                }
            } else {
                kini += b
            }
        }
        if (kini.isNotEmpty()) hasil += kini.toList()
        return hasil
    }

    /** Tabel dari satu blok baris; null bila barisnya tak cukup untuk jadi tabel. */
    private fun tabel(p: List<String>): Blok? {
        val baris = p.map { selTabel(it) }
            .filterNot { sel -> sel.isNotEmpty() && sel.all { pemisahTabel.matches(it) } }
        return when {
            baris.size >= 2 -> Blok.Tabel(baris.first(), baris.drop(1))
            else -> null
        }
    }

    private fun selTabel(baris: String): List<String> =
        baris.trim().removePrefix("|").removeSuffix("|").split("|").map { rapi(it) }
}
