package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nama sebuah alur kerja adalah dokumentasi yang dibaca kontributor *sebelum* membuka berkasnya.
 * `e2e.yml` pernah bernama *"E2E (mahal — hanya main/nightly/label e2e)"* padahal tak punya pemicu
 * `pull_request: types: [labeled]` sama sekali: menambahkan label `e2e` pada sebuah PR diam-diam
 * tidak menjalankan apa pun, dan yang menunggu hanya menyimpulkan testnya menggantung.
 *
 * Uji ini membaca kata pemicu di baris `name:` tiap alur kerja dan menuntut penandanya benar-benar
 * ada di blok `on:`.
 */
class NamaAlurKerjaJujurTest {

    private fun repo(): File =
        generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, ".github/workflows").isDirectory }
            ?: error(".github/workflows tidak ditemukan dari ${KodeSumber.root()}")

    private val alurKerja: List<File>
        get() = File(repo(), ".github/workflows").listFiles { f -> f.extension == "yml" }
            ?.sortedBy { it.name }.orEmpty()

    /** Blok `on:` sebuah alur kerja, dari `on:` sampai kunci tingkat-atas berikutnya. */
    private fun blokOn(teks: String): String {
        val mulai = Regex("(?m)^on:\\s*$").find(teks)?.range?.last
            ?: error("blok 'on:' tidak ditemukan")
        val sisa = teks.substring(mulai + 1)
        val akhir = listOf("permissions:", "concurrency:", "env:", "jobs:")
            .mapNotNull { Regex("(?m)^$it").find(sisa)?.range?.first }
            .minOrNull() ?: sisa.length
        return sisa.substring(0, akhir)
    }

    /** Kata pemicu di nama alur kerja → penanda yang harus ada di blok `on:`. */
    private val pemicu: Map<String, String> = mapOf(
        "label" to "labeled",
        "nightly" to "schedule:",
        "tag" to "tags:",
        "dispatch" to "workflow_dispatch:",
    )

    @Test
    fun namaAlurKerjaTidakMenjanjikanPemicuYangTidakAda() {
        assertTrue("tidak ada berkas alur kerja yang terbaca", alurKerja.size >= 5)
        val keliru = mutableListOf<String>()
        alurKerja.forEach { f ->
            val teks = f.readText()
            val nama = Regex("(?m)^name:\\s*(.+)$").find(teks)?.groupValues?.get(1) ?: return@forEach
            val on = blokOn(teks)
            pemicu.forEach { (kata, penanda) ->
                if (nama.contains(kata, ignoreCase = true) && !on.contains(penanda)) {
                    keliru += "${f.name}: nama menyebut '$kata' tetapi 'on:' tak memuat '$penanda'"
                }
            }
        }
        assertTrue(
            "nama alur kerja menjanjikan pemicu yang tak ada di berkasnya — kontributor menunggu " +
                "sesuatu yang tak akan pernah jalan: $keliru",
            keliru.isEmpty(),
        )
    }
}
