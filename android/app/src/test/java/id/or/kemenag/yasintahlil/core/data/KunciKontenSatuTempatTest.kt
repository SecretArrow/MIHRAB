package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Ejaan kunci konten harus hidup di **satu tempat**, dan tempat itu harus sama bagi yang memeriksa
 * maupun yang membaca.
 *
 * Kunci konten punya dua ejaan yang diterima skema — `sumber_url`/`sumberUrl` dan
 * `rujukan_file`/`rujukanFile`, ditambah larik `rujukan`. Dahulu setiap peran menuliskan daftarnya
 * sendiri, dan justru **pembacanya** yang tertinggal: `BundelKonten` dan gerbang CI mengenal
 * `sumberUrl`, sementara `ContentRepo` hanya membaca `sumber_url`; `dzikir()` mengenal
 * `rujukanFile`, sementara Tahlil dan bacaan sholat tidak. Akibatnya bundel bercampur ejaan itu
 * **lolos pemasangan** dan dilaporkan *"terpasang"*, lalu tiap berkasnya ditolak pembaca saat
 * dibuka (tanpa sumber resmi, berkas pembaruan dianggap tak bersumber) dan aplikasi diam-diam
 * kembali ke konten bawaan — pembaruan yang tampak berhasil tanpa mengubah apa pun, tanpa satu pun
 * pesan yang menyebut sebabnya. Pada Tahlil dan bacaan sholat hasilnya teks **kosong tanpa galat**:
 * rujukannya sudah "diperiksa" sebagai sah, tetapi tidak pernah dibaca.
 *
 * Uji ini menjaga agar daftar ejaan itu tidak kembali disalin: ejaan hanya boleh ditulis di
 * [id.or.kemenag.yasintahlil.core.util.ContentValidator] (sumber) dan di `Rujukan.kt` (rujukan);
 * seluruh pembaca dan pemeriksa memakainya lewat fungsi bersama.
 */
class KunciKontenSatuTempatTest {

    private val core = "src/main/java/id/or/kemenag/yasintahlil/core"

    private val repo = "$core/data/ContentRepo.kt"
    private val bundel = "$core/data/BundelKonten.kt"
    private val manager = "$core/data/ContentUpdateManager.kt"
    private val rujukan = "$core/data/Rujukan.kt"
    private val validator = "$core/util/ContentValidator.kt"

    /** Seluruh ejaan kunci konten, sebagaimana ditulis di dalam kode. */
    private val ejaan = listOf(
        "\"sumber_url\"", "\"sumberUrl\"",
        "\"rujukan_file\"", "\"rujukanFile\"", "\"rujukan\"",
    )

    /** Berkas sumber tanpa komentar — KDoc yang menjelaskan pola lama memuat tulisan yang sama. */
    private fun kode(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(relatif))

    private fun ejaanLokal(relatif: String): List<String> = ejaan.filter { kode(relatif).contains(it) }

    @Test
    fun ejaanSumberHanyaDitetapkanDiContentValidator() {
        assertTrue("ContentValidator tidak lagi menetapkan ejaan sumber",
            ejaanLokal(validator).contains("\"sumber_url\""))
        assertTrue("ContentValidator tidak lagi menetapkan ejaan kedua",
            ejaanLokal(validator).contains("\"sumberUrl\""))
        listOf(repo, bundel, manager).forEach { berkas ->
            val lokal = ejaanLokal(berkas).filter { it.contains("sumber", ignoreCase = true) }
            assertEquals("$berkas menulis ulang ejaan kunci sumber", emptyList<String>(), lokal)
        }
    }

    @Test
    fun ejaanRujukanHanyaDitetapkanDiRujukan() {
        assertTrue("Rujukan.kt tidak lagi menetapkan ejaan rujukan",
            ejaanLokal(rujukan).contains("\"rujukan_file\""))
        assertTrue("Rujukan.kt tidak lagi menetapkan ejaan kedua",
            ejaanLokal(rujukan).contains("\"rujukanFile\""))
        assertTrue("Rujukan.kt tidak lagi menetapkan larik rujukan",
            ejaanLokal(rujukan).contains("\"rujukan\""))
        listOf(repo, bundel, manager).forEach { berkas ->
            val lokal = ejaanLokal(berkas).filter { it.contains("rujukan", ignoreCase = true) }
            assertEquals("$berkas menulis ulang ejaan kunci rujukan", emptyList<String>(), lokal)
        }
    }

    /** Pemeriksa dan pembaca memakai aturan bersama, bukan ejaan yang ditulis sendiri. */
    @Test
    fun pembacaDanPemeriksaMemakaiAturanBersama() {
        assertTrue("ContentRepo tidak memakai ContentValidator.sumberBerkas()",
            kode(repo).contains("ContentValidator.sumberBerkas("))
        assertTrue("ContentRepo tidak memakai rujukanTeks()",
            kode(repo).contains("rujukanTeks(o)"))
        assertTrue("ContentRepo tidak memakai rujukanTeksSemua() untuk larik dzikir",
            kode(repo).contains("rujukanTeksSemua(o)"))
        assertTrue("BundelKonten tidak memakai ContentValidator.sumberBerkas()",
            kode(bundel).contains("ContentValidator.sumberBerkas("))
        assertTrue("penelusuran rujukan BundelKonten tidak memakai daftar ejaan bersama",
            kode(bundel).contains("k in KUNCI_RUJUKAN"))
        assertTrue("BundelKonten tidak memakai kunci larik rujukan bersama",
            kode(bundel).contains("KUNCI_RUJUKAN_DAFTAR"))
    }

    /** Daftar ejaan tidak boleh kembali dideklarasikan di luar [validator] dan [rujukan]. */
    @Test
    fun daftarEjaanTidakDideklarasikanUlang() {
        assertFalse("BundelKonten mendeklarasikan ulang daftar ejaan rujukan",
            kode(bundel).contains("val KUNCI_RUJUKAN"))
        assertFalse("BundelKonten mendeklarasikan ulang daftar ejaan sumber",
            kode(bundel).contains("val KUNCI_SUMBER"))
        assertFalse("ContentRepo mendeklarasikan daftar ejaan sendiri",
            kode(repo).contains("val KUNCI_"))
    }

    /**
     * Gate CI bundel (`tools/validate_bundle.py`) pun harus menerima kedua ejaan sumber — kalau
     * tidak, bundel yang diterima aplikasi justru ditolak CI, atau sebaliknya.
     */
    @Test
    fun gateCiBundelMenerimaKeduaEjaanSumber() {
        // Uji berjalan dengan direktori kerja modul (`app/`), jadi naik dulu ke akar repositori —
        // di sana `app/` dan `tools/` bersebelahan.
        val akar = generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_bundle.py").isFile }
            ?: error("tools/validate_bundle.py tidak ditemukan dari ${File(".").absolutePath}")
        val teks = File(akar, "tools/validate_bundle.py").readText()
        assertTrue("gate CI bundel tidak mengenal ejaan 'sumber_url'", "\"sumber_url\"" in teks)
        assertTrue("gate CI bundel tidak mengenal ejaan 'sumberUrl'", "\"sumberUrl\"" in teks)
    }
}
