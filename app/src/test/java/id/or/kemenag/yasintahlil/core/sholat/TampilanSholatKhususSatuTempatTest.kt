package id.or.kemenag.yasintahlil.core.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.Bacaan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dua layar sholat mayit — Ghaib dan Jenazah — menampilkan **medan yang sama** dari berkas
 * kontennya, jadi perangkaian teksnya harus satu tempat.
 *
 * Sebelum `TampilanSholatKhusus` ada, tiap layar merangkainya sendiri dan salinannya sudah
 * menyimpang tanpa ketahuan: syarat yang sama dipisah `"; "` di Ghaib tetapi `", "` lalu diberi
 * titik di Jenazah; dalil yang dimuat `sholat_jenazah.json` tak pernah tampil; judul layar
 * menyebut *"tanpa ruku/sujud"* di Ghaib saja; dan niat ditampilkan tanpa terjemahan di Ghaib
 * tetapi dengan terjemahan di Jenazah. Tak satu pun perbedaan itu tercatat sebagai pilihan.
 *
 * Uji ini menjaga dua hal yang berlawanan arah: (1) aturan yang **sama** tidak boleh dieja ulang di
 * layar mana pun, dan (2) perbedaan yang **disengaja** — judul langkah takbir dan label chip layar
 * penuh — harus tetap berbeda, supaya tidak "dirapikan" menjadi salinan.
 */
class TampilanSholatKhususSatuTempatTest {

    private fun root() = KodeSumber.root()

    private fun layar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private val ghaib get() = layar("feature/sholat/GhaibScreen.kt")
    private val jenazah get() = layar("feature/sholat/JenazahScreen.kt")

    // ---- Aturan bersama -------------------------------------------------------------------------

    /** Kedua layar harus memanggil tiap aturan bersama — bukan hanya sebagian. */
    @Test
    fun keduaLayarMemakaiAturanBersama() {
        val aturan = listOf("judul(", "langkah(", "syarat(", "dalil(", "catatanMazhab(", "teksBacaan(")
        mapOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            aturan.forEach { a ->
                assertTrue("$nama tidak memakai TampilanSholatKhusus.$a untuk teks yang sama",
                    teks.contains("TampilanSholatKhusus.$a"))
            }
        }
    }

    /**
     * Aturan bersama itu tidak boleh dieja ulang sebagai literal di layarnya. Yang dicari adalah
     * awalan labelnya: salinan yang menyimpang tetap memuat awalan yang sama, dan itulah yang
     * membuat dua layar menyebut hal yang sama dengan cara berbeda.
     */
    @Test
    fun aturanBersamaTidakDiejaUlangDiLayar() {
        val terlarang = listOf("Syarat: ", "Dalil: ", "Catatan mazhab: ", "\${i + 1}. ")
        mapOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            terlarang.forEach { t ->
                assertTrue("$nama mengeja ulang \"$t\" alih-alih memakai TampilanSholatKhusus",
                    !teks.contains(t))
            }
        }
    }

    /** Perangkaian syarat dipaku: satu pemisah dan satu penutup untuk kedua layar. */
    @Test
    fun perangkaianSyaratSatuAturan() {
        assertEquals("Syarat: a; b; c.",
            TampilanSholatKhusus.syarat(listOf("a", "b", "c")))
        assertEquals("Syarat: satu.", TampilanSholatKhusus.syarat(listOf("satu")))
    }

    /** Judul, langkah, dalil, dan catatan mazhab dipaku ejaannya. */
    @Test
    fun ejaanAturanBersamaTidakBerubah() {
        assertEquals("Sholat Ghaib — 4 takbir, tanpa ruku/sujud",
            TampilanSholatKhusus.judul("Sholat Ghaib", 4))
        assertEquals("3. Takbir 3 → Doa untuk mayit",
            TampilanSholatKhusus.langkah(3, "Takbir 3 → Doa untuk mayit"))
        assertEquals("Dalil: HR contoh.", TampilanSholatKhusus.dalil("HR contoh."))
        assertEquals("Catatan mazhab: Syafi'i.", TampilanSholatKhusus.catatanMazhab("Syafi'i."))
    }

    /** Cadangan teks bacaan: hasil rujukan bersama bila ada, medan entri bila rujukannya kosong. */
    @Test
    fun cadanganTeksBacaanSatuAturan() {
        val b = Bacaan(id = "x", judul = "X", arab = "arab-entri", latin = "latin-entri", terjemahan = "terj-entri")
        assertEquals(Triple("arab-rujukan", "latin-rujukan", "terj-rujukan"),
            TampilanSholatKhusus.teksBacaan(b, "arab-rujukan", "latin-rujukan", "terj-rujukan"))
        assertEquals(Triple("arab-entri", "latin-entri", "terj-entri"),
            TampilanSholatKhusus.teksBacaan(b, "", "", ""))
    }

    // ---- Perbedaan yang disengaja ---------------------------------------------------------------

    /**
     * Judul langkah takbir memang khas tiap layar — Ghaib membaca ta'awudz sebelum Al-Fatihah dan
     * salamnya menoleh dua sisi, Jenazah tidak. Uji ini memakunya: bila kelak "dirapikan" menjadi
     * satu peta bersama, perbedaan yang disengaja itu hilang tanpa ketahuan.
     */
    @Test
    fun judulLangkahTetapKhasTiapLayar() {
        val pola = Regex("""BacaanSholat\.(\w+) to "([^"]*)"""")
        fun peta(teks: String): Map<String, String> =
            pola.findAll(teks).associate { it.groupValues[1] to it.groupValues[2] }
        val g = peta(ghaib)
        val j = peta(jenazah)
        assertEquals("kedua layar tidak lagi menjuduli urutan id yang sama",
            g.keys, j.keys)
        val berbeda = g.filter { (k, v) -> j[k] != v }.keys
        assertEquals("perbedaan judul langkah di luar yang disengaja (ta'awudz & arah salam)",
            setOf("FATIHAH", "SALAM"), berbeda)
    }

    /** Label chip layar penuh juga sengaja berbeda: pada Ghaib, layar penuh sekaligus mode imam. */
    @Test
    fun labelLayarPenuhSengajaBerbeda() {
        assertTrue("Ghaib tidak lagi memakai LAYAR_PENUH_IMAM",
            ghaib.contains("NamaAntarmuka.LAYAR_PENUH_IMAM"))
        assertTrue("Jenazah tidak lagi memakai LAYAR_PENUH",
            jenazah.contains("NamaAntarmuka.LAYAR_PENUH)"))
    }

    // ---- Konten yang tak boleh terbuang ----------------------------------------------------------

    /**
     * Dalil yang dimuat `sholat_jenazah.json` dulu tak pernah tampil. Uji ini menuntut medannya
     * **ada isinya** dan **ditampilkan** — tanpa prasyarat isinya, penjaganya bisa dipuaskan dengan
     * mengosongkan medannya.
     */
    @Test
    fun dalilJenazahDitampilkan() {
        val isi = KodeSumber.sumber("src/main/assets/content/sholat_jenazah.json")
        assertTrue("sholat_jenazah.json tidak lagi memuat dalil — penjaga tampilannya jadi hampa",
            Regex(""""dalil"\s*:\s*"[^"]+"""").containsMatchIn(isi))
        assertTrue("layar Jenazah tidak menampilkan dalil yang dimuat berkas kontennya",
            jenazah.contains("TampilanSholatKhusus.dalil("))
    }

    /**
     * Terjemahan niat ditampilkan di **kedua** layar. Sebelumnya Ghaib memanggil `BacaanCard`
     * dengan `showTrans = false` padahal berkasnya memuat terjemahan — konten yang ada tetapi tak
     * terbaca, dan tak ada alasan yang tercatat.
     */
    @Test
    fun terjemahanNiatTampilDiKeduaLayar() {
        // Jarak baris sengaja tidak dipaku: yang dijaga adalah pasangan `n.terjemahan` lalu
        // `showLatin = true, showTrans = true` pada kartu niat yang sama, bukan lekuk indentasinya.
        val kartuNiatDenganTerjemahan = Regex("""n\.terjemahan,\s*\n\s*true,\s*true,""")
        listOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            assertTrue("$nama tidak menampilkan terjemahan niat (showTrans bukan true)",
                kartuNiatDenganTerjemahan.containsMatchIn(teks))
        }
    }
}
