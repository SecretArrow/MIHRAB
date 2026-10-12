package id.or.kemenag.yasintahlil.core.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `entriRujukan` menyelesaikan `rujukan_file` = `berkas.json#id`. Rujukan itu hidup di tiga
 * bentuk berkas konten: `bagian` (Tahlil), `bacaan` (sholat jenazah & langkah sholat), dan
 * `kelompok[].items[]` (dzikir). Bentuk ketiga ditambahkan agar pustaka dzikir dapat menunjuk
 * entrinya sendiri — kelompok "Doa memohon ampunan" menunjuk Sayyidul Istighfar yang sudah
 * terverifikasi di kelompok "Dzikir pagi & petang", tanpa menyalin lafaznya.
 *
 * `ContentRepo` sendiri tidak dapat diuji di JVM (ia membaca assets lewat `Context`), jadi
 * penyelesainya dipisahkan ke fungsi murni ini dan diuji langsung.
 */
class RujukanTest {

    @Test
    fun menemukanEntriDiBagian() {
        val d = JSONObject("""{"bagian":[{"id":"a","arab":"satu"},{"id":"b","arab":"dua"}]}""")
        assertEquals("dua", entriRujukan(d, "b")?.optString("arab"))
    }

    @Test
    fun menemukanEntriDiBacaan() {
        val d = JSONObject("""{"bacaan":[{"id":"a"},{"id":"z","terjemahan":"t"}]}""")
        assertEquals("t", entriRujukan(d, "z")?.optString("terjemahan"))
    }

    /** Bentuk ketiga — ditambahkan agar pustaka dzikir bisa menunjuk entrinya sendiri. */
    @Test
    fun menemukanEntriDiDalamKelompok() {
        val d = JSONObject(
            """{"kelompok":[{"id":"g1","items":[{"id":"p","arab":"p"}]},
                {"id":"g2","items":[{"id":"q","arab":"q"}]}]}"""
        )
        assertEquals("q", entriRujukan(d, "q")?.optString("arab"))
        assertEquals("p", entriRujukan(d, "p")?.optString("arab"))
    }

    /** Rujukan mati harus jadi `null` — pemanggil memperlakukannya sebagai teks kosong. */
    @Test
    fun idTakDikenalMenghasilkanNull() {
        val d = JSONObject("""{"bagian":[{"id":"a"}],"kelompok":[{"id":"g","items":[{"id":"p"}]}]}""")
        assertNull(entriRujukan(d, "tidak_ada"))
    }

    @Test
    fun idKosongMenghasilkanNull() {
        val d = JSONObject("""{"bagian":[{"id":"a"}]}""")
        assertNull(entriRujukan(d, ""))
        assertNull(entriRujukan(d, "   "))
    }

    /** Berkas tanpa daftar entri apa pun tidak boleh melempar — hanya menghasilkan `null`. */
    @Test
    fun dokumenTanpaDaftarEntriTidakMelempar() {
        assertNull(entriRujukan(JSONObject("""{"judul":"x"}"""), "a"))
    }

    /** `bagian` dicari lebih dahulu; id yang hanya ada di `kelompok` tetap ditemukan. */
    @Test
    fun mencariDiBagianDuluLaluKelompok() {
        val d = JSONObject(
            """{"bagian":[{"id":"a","arab":"bagian"}],
                "kelompok":[{"id":"g","items":[{"id":"a","arab":"kelompok"}]}]}"""
        )
        assertEquals("bagian", entriRujukan(d, "a")?.optString("arab"))
    }

    /**
     * `ContentRepo` wajib memakai penyelesai ini. Bila ia menyalin logikanya sendiri, rujukan
     * `dzikir.json#id` berhenti bekerja tanpa satu pun uji perilaku yang menangkapnya — dan
     * `ContentRepo` tak bisa dijalankan di uji JVM karena butuh `Context` untuk membaca assets.
     */
    @Test
    fun contentRepMemakaiPenyelesaiBersama() {
        val relatif = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"
        val src = listOf(File("."), File("app")).map { File(it, relatif) }
            .firstOrNull { it.isFile }?.readText()
            ?: error("$relatif tidak ditemukan dari ${File(".").absolutePath}")
        assertTrue("ContentRepo tidak memakai entriRujukan() — logika rujukan bisa menyimpang",
            src.contains("entriRujukan("))
    }
}
