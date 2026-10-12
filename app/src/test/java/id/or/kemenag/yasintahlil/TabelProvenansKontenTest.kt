package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `docs/CONTENT_SOURCES.md` menyatakan dirinya mencatat *"asal setiap file konten"*. Tabelnya
 * ditulis tangan, terpisah dari berkas yang benar-benar dikirim di dalam APK — dan **tak ada apa
 * pun yang membandingkan keduanya**.
 *
 * Tabel provenans adalah tempat orang memeriksa dari mana sebuah berkas berasal sebelum
 * mempercayainya. Bila tabelnya tidak lengkap, berkas yang tak tercatat justru yang paling sulit
 * ditemukan: ia ikut terkirim, terbaca aplikasi, dan tak seorang pun pernah memeriksa asalnya
 * karena tabel — satu-satunya daftar yang ada — tidak menyebutnya. Sebaliknya, baris yang menunjuk
 * berkas yang sudah dihapus membuat tabel tampak lebih lengkap daripada kenyataan, dan pembaca
 * menyimpulkan ada berkas yang sebenarnya tidak pernah ada.
 *
 * Uji ini menutup kedua arah: setiap berkas konten wajib tercatat, dan setiap nama di tabel wajib
 * benar-benar ada. Isinya sendiri (status ✅/⚠️, nama sumber) tidak diperiksa di sini — yang dijaga
 * adalah **kelengkapan daftarnya**, sebab daftar yang tidak lengkap membuat seluruh kolom lainnya
 * tak bermakna.
 */
class TabelProvenansKontenTest {

    private fun modul(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun repo(): File =
        listOf(File(".."), File(".")).first { File(it, "docs/CONTENT_SOURCES.md").isFile }

    private val berkasKonten: List<String>
        get() = File(modul(), "src/main/assets/content")
            .listFiles { f -> f.extension == "json" }?.map { it.name }?.sorted().orEmpty()

    private val dokumen: String get() = File(repo(), "docs/CONTENT_SOURCES.md").readText()

    /**
     * Nama berkas dari sel pertama sebuah baris provenans.
     *
     * Sel dapat memuat satu nama (`yasin_full.json`) atau **bentuk singkat** yang mengumpulkan
     * berkas sejenis dalam satu baris (`surah_001/112/113/114.json`). Bentuk singkat dibaca
     * sebagai: segmen pertama memberi awalan hurufnya (`surah_`), sisanya adalah varian — jadi
     * `112` menjadi `surah_112.json`. Kedua bentuk diterima supaya pilihan gaya penulisan tabel
     * tidak pernah menjadi alasan uji ini gagal.
     */
    private fun namaBerkas(sel: String): List<String> {
        val token = Regex("`([^`]+)`").find(sel)?.groupValues?.get(1) ?: return emptyList()
        fun berkasJson(s: String) = if (s.endsWith(".json")) s else "$s.json"

        val segmen = token.split("/")
        if (segmen.size == 1) return listOf(token)
        val awalan = Regex("^[a-z_]+").find(segmen.first())?.value
            ?: error("bentuk singkat '$token' tidak diawali huruf sehingga tak dapat dibaca")
        // Segmen pertama adalah nama utuh; sisanya varian yang mewarisi awalannya.
        return listOf(berkasJson(segmen.first())) +
            segmen.drop(1).map { awalan + berkasJson(it) }
    }

    /**
     * Baris provenans = baris tabel yang sel pertamanya **hanya** sebuah nama berkas `.json`.
     *
     * Tabel lain di dokumen yang sama (kategori bacaan, kekurangan yang menunggu sumber) tidak ikut
     * terbaca: sel pertamanya bukan satu nama berkas. Tanpa penyaring ini, nama berkas yang
     * disebut di sel mana pun akan terhitung "tercatat" — padahal yang menjanjikan kelengkapan
     * adalah tabel provenansnya, bukan penyebutan sambil lalu di prosa tabel lain.
     */
    private val namaDiTabel: List<String>
        get() = dokumen.lines()
            .filter { it.startsWith("|") }
            .mapNotNull { baris ->
                val sel = baris.trim().trimStart('|').substringBefore('|').trim()
                if (!Regex("^`[^`]+\\.json(/[^`]*)?`$").matches(sel)) return@mapNotNull null
                namaBerkas(sel)
            }
            .flatten()

    @Test
    fun setiapBerkasKontenTercatatDiTabelProvenans() {
        assertTrue("berkas konten tidak ditemukan", berkasKonten.size >= 15)
        assertTrue("baris provenans tidak terbaca dari docs/CONTENT_SOURCES.md", namaDiTabel.size >= 15)

        val takTercatat = berkasKonten.toSet() - namaDiTabel.toSet()
        assertTrue(
            "berkas konten ikut dikirim di APK tetapi tidak tercatat di tabel provenans " +
                "docs/CONTENT_SOURCES.md — asalnya tak pernah diperiksa siapa pun: $takTercatat",
            takTercatat.isEmpty(),
        )
    }

    @Test
    fun setiapBerkasDiTabelProvenansBenarBenarAda() {
        val hantu = namaDiTabel.toSet() - berkasKonten.toSet()
        assertTrue(
            "tabel provenans menyebut berkas yang tidak ada di assets/content — tabel tampak " +
                "lebih lengkap daripada kenyataan: $hantu",
            hantu.isEmpty(),
        )
        assertEquals("jumlah berkas di tabel berbeda dari yang benar-benar dikirim",
            berkasKonten.size, namaDiTabel.toSet().size)
    }

    /** Bentuk singkat harus benar-benar dikembangkan — pengurai yang berhenti mengenalinya membuat uji ini hijau tanpa memeriksa apa pun. */
    @Test
    fun bentukSingkatDiuraikanMenjadiSeluruhNamanya() {
        assertEquals(
            listOf("surah_001.json", "surah_112.json", "surah_113.json", "surah_114.json"),
            namaBerkas("`surah_001/112/113/114.json`"),
        )
        assertEquals(listOf("yasin_full.json"), namaBerkas("`yasin_full.json`"))
        assertEquals(emptyList<String>(), namaBerkas("Doa **keluar masjid**"))
    }
}
