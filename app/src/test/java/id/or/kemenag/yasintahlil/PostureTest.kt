package id.or.kemenag.yasintahlil.feature.sholat

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga kelengkapan ilustrasi gerakan sholat (plan.txt): setiap bacaan pada
 * `sholat_bacaan.json` harus punya skema posisi + deskripsi posisi tubuh. Deskripsi
 * wajib prosedural (tanpa lafaz Arab), dan berkas skema harus benar-benar ada.
 */
class PostureTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun contentFile(name: String): File = File(root(), "src/main/assets/content/$name")

    private fun drawable(name: String): File = File(root(), "src/main/res/drawable/$name")

    @Test
    fun setiap_bacaan_punya_posisi_bergambar() {
        val json = contentFile("sholat_bacaan.json").readText()
        val ids = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json)
            .map { it.groupValues[1] }.toList()
        assertTrue("Tidak ada id bacaan ditemukan", ids.isNotEmpty())

        val tanpaPosisi = ids.filter { Postures.of(it) == null }
        assertTrue("Bacaan tanpa posisi: $tanpaPosisi", tanpaPosisi.isEmpty())

        ids.forEach { id ->
            val p = Postures.of(id)!!
            assertTrue("$id: gambar kosong", p.gambar != 0)
            assertTrue("$id: label kosong", p.label.isNotBlank())
            assertTrue("$id: deskripsi posisi kosong", p.deskripsi.isNotBlank())
            // Deskripsi harus prosedural, bukan lafaz — tolak rentang Arab.
            assertTrue("$id: deskripsi mengandung teks Arab",
                p.deskripsi.none { it.code in 0x0600..0x06FF })
            assertTrue("$id: label mengandung teks Arab",
                p.label.none { it.code in 0x0600..0x06FF })
        }
    }

    @Test
    fun berkas_skema_posisi_tersedia() {
        listOf(
            "posisi_berdiri", "posisi_takbir", "posisi_rukuk",
            "posisi_sujud", "posisi_duduk", "posisi_salam",
        ).forEach { nama ->
            val f = drawable("$nama.xml")
            assertTrue("$nama.xml tidak ada", f.isFile)
            // Skema tidak boleh memuat teks Arab (hanya geometri).
            assertTrue("$nama.xml memuat teks Arab",
                f.readText().none { it.code in 0x0600..0x06FF })
        }
    }

    @Test
    fun cakupan_posisi_tidak_memuat_id_tak_dikenal() {
        val json = contentFile("sholat_bacaan.json").readText()
        val ids = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json)
            .map { it.groupValues[1] }.toSet()
        val asing = Postures.coveredIds() - ids
        assertTrue("Posisi untuk bacaan tak dikenal: $asing", asing.isEmpty())
    }
}
