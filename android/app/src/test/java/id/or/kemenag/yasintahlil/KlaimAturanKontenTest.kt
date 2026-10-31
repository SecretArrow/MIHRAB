package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `SOURCES.md` §"Aturan konten" pernah menjanjikan *"Setiap entri JSON wajib: `arab, latin,
 * terjemahan, sumber_url, edisi, catatan_mazhab`"*. Tak satu pun pemeriksa memeriksa `edisi` atau
 * `catatan_mazhab` — `tools/validate_content.py` bahkan tak pernah menyebut namanya — dan konten
 * yang benar-benar dikirim pun tidak memuatnya di semua berkas (keduanya metadata provenans di
 * tingkat berkas/bagian, bukan per-entri). Pembaca yang mempercayai aturan itu mengira CI akan
 * menangkap kutipan yang hilang, padahal tidak.
 *
 * Uji ini menjaga agar dokumen tidak lagi menjanjikan pemeriksaan yang tidak ada: setiap bidang
 * yang **dinyatakan wajib** harus benar-benar disebut gate, dan setiap bidang yang **dinyatakan
 * tidak diperiksa mesin** harus benar-benar tidak disebut gate. Kedua arah penting — dokumen yang
 * menjanjikan lebih dari kenyataan membuat orang lengah, yang menjanjikan kurang dari kenyataan
 * membuat orang memeriksa ulang hal yang sudah otomatis.
 */
class KlaimAturanKontenTest {

    private fun repo(): File =
        generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${KodeSumber.root()}")

    private fun berkas(relatif: String): String = File(repo(), relatif).readText()

    /** Butir-butir aturan konten (`- …`) dari `SOURCES.md`. */
    private fun aturan(): List<String> = berkas("SOURCES.md").lines()
        .filter { it.trimStart().startsWith("- ") }

    /** Nama bidang dalam tanda kutip-belakang, hanya bila isinya satu pengenal (`sumber_url`). */
    private fun bidang(baris: String): Set<String> =
        Regex("`([a-z][a-z_]*)`").findAll(baris).map { it.groupValues[1] }.toSet()

    private val barisWajib: List<String> get() = aturan().filter { it.contains("wajib") }
    private val barisTakDiperiksa: List<String> get() =
        aturan().filter { it.contains("Tidak diperiksa mesin") }

    @Test
    fun bidangYangDijanjikanWajibBenarBenarDiperiksaGate() {
        assertTrue("tidak ada butir 'wajib' yang terbaca di SOURCES.md", barisWajib.isNotEmpty())
        val dijanjikan = barisWajib.flatMap { bidang(it) }.toSet()
        assertTrue("tidak ada nama bidang yang terbaca dari aturan wajib", dijanjikan.isNotEmpty())

        val gate = berkas("tools/validate_content.py")
        val takDiperiksa = dijanjikan.filterNot { gate.contains(it) }
        assertTrue(
            "SOURCES.md menyatakan bidang ini wajib, tetapi tools/validate_content.py tak pernah " +
                "menyebutnya — janji pemeriksaan yang tidak ada: $takDiperiksa",
            takDiperiksa.isEmpty(),
        )
    }

    @Test
    fun bidangYangDinyatakanTidakDiperiksaMemangTidakDiperiksa() {
        assertTrue(
            "butir 'Tidak diperiksa mesin' tak terbaca di SOURCES.md",
            barisTakDiperiksa.isNotEmpty(),
        )
        val dinyatakan = barisTakDiperiksa.flatMap { bidang(it) }.toSet()
        assertTrue(
            "tidak ada nama bidang yang terbaca dari pernyataan 'tidak diperiksa'",
            dinyatakan.isNotEmpty(),
        )

        val gate = berkas("tools/validate_content.py")
        val ternyataDiperiksa = dinyatakan.filter { gate.contains(it) }
        assertTrue(
            "SOURCES.md menyatakan bidang ini tidak diperiksa mesin, padahal gate menyebutnya — " +
                "dokumen meremehkan gate: $ternyataDiperiksa",
            ternyataDiperiksa.isEmpty(),
        )
    }

    /** Aturan yang paling mahal bila hilang tanpa suara: larangan mengarang teks agama. */
    @Test
    fun laranganMengarangTeksAgamaMasihAda() {
        val sumber = berkas("SOURCES.md")
        assertTrue(
            "SOURCES.md tak lagi melarang membuat teks Arab dengan AI",
            sumber.contains("Dilarang generate teks Arab dengan AI"),
        )
        assertTrue(
            "SOURCES.md tak lagi melarang mengarang hadis/sanad",
            sumber.contains("Dilarang karang hadis/sanad"),
        )
    }
}
