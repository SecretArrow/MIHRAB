package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        val src = KodeSumber.sumber(REPO)
        assertTrue("ContentRepo tidak memakai entriRujukan() — logika rujukan bisa menyimpang",
            src.contains("entriRujukan("))
    }

    // --- Ejaan kunci rujukan: pemeriksa dan pembaca harus sepakat ---------------------------------

    /**
     * Rujukan berkas dapat ditulis dengan dua ejaan. Sebelum ini, hanya `dzikir()` yang mengenal
     * ejaan `rujukanFile`; Tahlil dan bacaan sholat membaca `rujukan_file` saja — sementara
     * pemeriksa bundel (`BundelKonten`) mengenal keduanya. Bundel ber-`rujukanFile` karena itu
     * lolos pemasangan dan dilaporkan *"terpasang"*, lalu entri yang mengandalkannya tampil
     * **kosong tanpa galat**: rujukannya sudah "diperiksa" sebagai sah tetapi tak pernah dibaca.
     */
    @Test
    fun rujukanTeksMengenaliKeduaEjaan() {
        assertEquals("a.json#x", rujukanTeks(JSONObject("""{"rujukan_file":"a.json#x"}""")))
        assertEquals("b.json#y", rujukanTeks(JSONObject("""{"rujukanFile":"b.json#y"}""")))
    }

    @Test
    fun rujukanTeksMenghasilkanNullBilaTidakMerujuk() {
        assertNull(rujukanTeks(JSONObject("""{"judul":"x","arab":"y"}""")))
    }

    /**
     * Bila kedua ejaan ada sekaligus, pembaca mengikuti **semuanya** — pemeriksa pun menelusuri
     * setiap kemunculan kunci rujukan, jadi pembaca yang hanya mengambil yang pertama akan
     * memeriksa sesuatu yang tidak pernah ia baca.
     */
    @Test
    fun rujukanTeksSemuaMengembalikanSetiapEjaanYangAda() {
        val o = JSONObject("""{"rujukan_file":"a.json","rujukanFile":"b.json"}""")
        assertEquals(listOf("a.json", "b.json"), rujukanTeksSemua(o))
        assertEquals("a.json", rujukanTeks(o))
    }

    @Test
    fun rujukanTeksSemuaKosongBilaTidakMerujuk() {
        assertTrue(rujukanTeksSemua(JSONObject("""{"judul":"x"}""")).isEmpty())
    }

    /** Larik `rujukan` bukan ejaan tunggal: ia dibaca terpisah oleh `dzikir()`. */
    @Test
    fun larikRujukanBukanBagianDariEjaanTunggal() {
        assertTrue(rujukanTeksSemua(JSONObject("""{"rujukan":["a.json"]}""")).isEmpty())
        assertTrue("'$KUNCI_RUJUKAN_DAFTAR' tidak boleh ikut sebagai ejaan tunggal",
            KUNCI_RUJUKAN.none { it == KUNCI_RUJUKAN_DAFTAR })
    }

    companion object {
        private const val REPO = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"
    }
}
