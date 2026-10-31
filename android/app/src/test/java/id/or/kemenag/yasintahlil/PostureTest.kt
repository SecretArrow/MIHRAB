package id.or.kemenag.yasintahlil.feature.sholat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga kelengkapan ilustrasi gerakan sholat: setiap bacaan pada `sholat_bacaan.json` harus
 * punya kartun posisi empat tampak (depan, kanan, kiri, belakang) + deskripsi posisi tubuh.
 * Deskripsi wajib prosedural (tanpa lafaz Arab), dan gambar itu sendiri tanpa teks — berkas
 * kartun tidak boleh memuat satu pun karakter Arab.
 *
 * Uji berkas memindai `Posture.kt` untuk nama `R.drawable.posisi_*` yang dipakai, sehingga
 * daftarnya tidak pernah basi: menambah varian baru otomatis ikut diperiksa, dan berkas
 * kartun yang tidak lagi dipakai (yatim) ikut tertangkap.
 */
class PostureTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun contentFile(name: String): File = File(root(), "src/main/assets/content/$name")

    private fun drawable(name: String): File = File(root(), "src/main/res/drawable/$name")

    private fun kodePosture(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/Posture.kt").readText()

    @Test
    fun setiap_bacaan_punya_posisi_empat_tampak() {
        val json = contentFile("sholat_bacaan.json").readText()
        val ids = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json)
            .map { it.groupValues[1] }.toList()
        assertTrue("Tidak ada id bacaan ditemukan", ids.isNotEmpty())

        val tanpaPosisi = ids.filter { Postures.of(it) == null }
        assertTrue("Bacaan tanpa posisi: $tanpaPosisi", tanpaPosisi.isEmpty())

        ids.forEach { id ->
            val p = Postures.of(id)!!
            assertTrue("$id: tanpa varian posisi", p.varian.isNotEmpty())
            p.varian.forEachIndexed { i, v ->
                assertTrue("$id varian $i: gambar depan kosong", v.tampak.depan != 0)
                assertTrue("$id varian $i: gambar kanan kosong", v.tampak.kanan != 0)
                assertTrue("$id varian $i: gambar kiri kosong", v.tampak.kiri != 0)
                assertTrue("$id varian $i: gambar belakang kosong", v.tampak.belakang != 0)
            }
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
    fun berkas_kartun_posisi_tersedia_dan_utuh() {
        val kode = kodePosture()
        val dipakai = Regex("R\\.drawable\\.(posisi_\\w+)")
            .findAll(kode).map { it.groupValues[1] }.toList()
        assertTrue("Tidak ada kartun posisi dirujuk di Posture.kt", dipakai.isNotEmpty())
        // 11 gerakan x 4 tampak = 44 kartun; salam menyumbang dua gerakan tolehan.
        assertEquals("jumlah kartun posisi yang dirujuk tidak utuh (harus 44)",
            44, dipakai.distinct().size)
        dipakai.distinct().forEach { nama ->
            val f = drawable("$nama.xml")
            assertTrue("$nama.xml tidak ada", f.isFile)
            // Kartun tanpa teks: tidak boleh memuat karakter Arab (hanya geometri).
            assertTrue("$nama.xml memuat teks Arab",
                f.readText().none { it.code in 0x0600..0x06FF })
        }
        // Berkas yatim — ada di drawable tapi tak dipakai mana pun — menandai sisa
        // ilustrasi lama yang terlupakan.
        val berkas = drawable(".").listFiles { f -> f.name.startsWith("posisi_") }
            ?.map { it.name.removeSuffix(".xml") } ?: emptyList()
        val yatim = berkas - dipakai.distinct().toSet()
        assertTrue("Berkas posisi tak terpakai: $yatim", yatim.isEmpty())
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
