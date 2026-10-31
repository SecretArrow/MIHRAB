package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `docs/CONTENT_UPDATE.md` punya bagian *"Sisi CI"* yang menjanjikan pembaca: inilah fixture yang
 * membuktikan gate konten benar-benar menolak. Bagian itu dulu menulis *"dua fixture"* dan hanya
 * menyebut `bundel/sah.json` serta `bundel/cacat.json` — padahal `ci.yml` sudah lama menjalankan
 * `bundel/bacaan-cacat.json`, dan sekarang juga seluruh fixture `konten/`. Pembaca yang
 * mempercayai dokumen itu menyimpulkan cakupan gate lebih sempit daripada kenyataan, dan tak
 * pernah tahu fixture mana yang harus ikut diperbarui saat gate berubah.
 *
 * Uji ini menjaga tiga sisi tetap sejalan: berkas fixture yang ada di disk, langkah yang
 * benar-benar dijalankan `ci.yml`, dan daftar yang ditulis dokumen.
 */
class FixtureCiTerdokumentasiTest {

    private fun repo(): File =
        generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${KodeSumber.root()}")

    private fun direktori(nama: String): List<File> =
        File(repo(), "tools/fixtures/$nama").listFiles { f -> f.extension == "json" }
            ?.sortedBy { it.name }.orEmpty()

    /** Seluruh fixture gate, sebagai jalur relatif terhadap `tools/fixtures/` (`bundel/sah.json`). */
    private val fixture: List<String>
        get() = listOf("bundel", "konten").flatMap { d -> direktori(d).map { "$d/${it.name}" } }

    private fun dokumen(): String = File(repo(), "docs/CONTENT_UPDATE.md").readText()
    private fun ci(): String = File(repo(), ".github/workflows/ci.yml").readText()

    @Test
    fun setiapFixtureBenarBenarDijalankanCi() {
        assertTrue("fixture tidak ditemukan", fixture.size >= 5)
        val takDijalankan = fixture.filterNot { ci().contains("tools/fixtures/$it") }
        assertTrue(
            "fixture ini ada di disk tetapi tak pernah dijalankan ci.yml — ia tampak menjaga " +
                "sesuatu padahal tidak: $takDijalankan",
            takDijalankan.isEmpty(),
        )
    }

    @Test
    fun setiapFixtureDisebutDokumenSisiCi() {
        val doc = dokumen()
        val takDisebut = fixture.filterNot { doc.contains(it) }
        assertTrue(
            "docs/CONTENT_UPDATE.md tidak menyebut fixture ini — pembaca tak tahu cakupan gate " +
                "yang sebenarnya: $takDisebut",
            takDisebut.isEmpty(),
        )
    }

    @Test
    fun dokumenTidakMenyebutFixtureYangTidakAda() {
        val disebut = Regex("tools/fixtures/([a-z]+/[A-Za-z0-9_-]+\\.json)")
            .findAll(dokumen()).map { it.groupValues[1] }.toSet()
        assertTrue("dokumen tidak menyebut satu pun berkas fixture", disebut.isNotEmpty())
        val hantu = disebut - fixture.toSet()
        assertTrue(
            "docs/CONTENT_UPDATE.md menyebut fixture yang tidak ada di disk — dokumen tampak " +
                "lebih lengkap daripada kenyataan: $hantu",
            hantu.isEmpty(),
        )
    }
}
