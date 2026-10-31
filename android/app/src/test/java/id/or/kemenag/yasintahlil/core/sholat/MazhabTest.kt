package id.or.kemenag.yasintahlil.core.sholat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Tata Cara Sholat Langkah demi Langkah": *"Pilihan mazhab atau rujukan"* dan
 * *"Perbedaan mazhab dicatat, tidak dicampuradukkan"*.
 *
 * Daftar mazhab dan labelnya dahulu ditulis di tiga tempat: `MAZHAB_OPTIONS` di layar panduan,
 * sebuah `listOf(...)` di layar Pengaturan, dan rantai `when` untuk kepala layar panduan. Dua daftar
 * yang berbeda berarti satu layar menawarkan mazhab yang tidak dikenal layar lain, dan label yang
 * berbeda untuk kunci yang sama membuat kepala layar menyebut mazhab yang tidak sedang dipilih.
 */
class MazhabTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    @Test
    fun daftarMazhabUnikDanLengkap() {
        val kunci = Mazhab.SEMUA.map { it.first }
        assertEquals("kunci mazhab tidak boleh ganda", kunci.size, kunci.toSet().size)
        assertEquals(listOf("syafii", "hanafi", "maliki", "hambali"), kunci)
        // Setiap kunci punya nama tampilan yang terisi.
        Mazhab.SEMUA.forEach { (k, label) ->
            assertTrue("label mazhab $k kosong", label.isNotBlank())
        }
    }

    @Test
    fun hanyaKunciYangTerdaftarDianggapSah() {
        Mazhab.SEMUA.forEach { assertTrue(Mazhab.sah(it.first)) }
        listOf("", "SYAFII", "Syafii", "syafi'i", "hanbali", " ").forEach {
            assertFalse("'$it' bukan kunci mazhab yang sah", Mazhab.sah(it))
        }
    }

    /**
     * Kunci tak dikenal (mis. dari backup yang disunting) memakai nama [Mazhab.DEFAULT], bukan kunci
     * mentahnya — supaya kepala layar tidak menampilkan istilah asing.
     */
    @Test
    fun labelTakDikenalJatuhKeMazhabBawaan() {
        assertEquals(Mazhab.label(Mazhab.DEFAULT), Mazhab.label(""))
        assertEquals(Mazhab.label(Mazhab.DEFAULT), Mazhab.label("entah-apa"))
        // Label tiap kunci yang sah dikembalikan apa adanya, bukan jatuhan.
        Mazhab.SEMUA.forEach { (k, label) -> assertEquals(label, Mazhab.label(k)) }
    }

    @Test
    fun mazhabBawaanAdalahYangTerdaftar() {
        assertTrue("bawaan harus salah satu kunci sah", Mazhab.sah(Mazhab.DEFAULT))
        assertEquals(Mazhab.SYAFII, Mazhab.DEFAULT)
    }

    /** Aturan bersama harus tetap murni: tanpa Android/Compose, agar dapat diuji langsung di JVM. */
    @Test
    fun aturanMazhabTidakMenyentuhAndroid() {
        val berkas = File(
            root(),
            "src/main/java/id/or/kemenag/yasintahlil/core/sholat/Mazhab.kt",
        ).readText()
        assertFalse("Mazhab mengimpor Android sehingga tak dapat diuji di JVM",
            berkas.contains("import android."))
        assertFalse("Mazhab mengimpor Compose sehingga tak dapat diuji di JVM",
            berkas.contains("import androidx.compose"))
    }

    /** Daftar mazhab tak boleh ditulis ulang di layar: satu daftar bersama untuk semua pemakai. */
    @Test
    fun daftarMazhabTidakDitulisUlangDiLayar() {
        val pelanggar = mutableListOf<String>()
        // Salinan daftar lama: pasangan kunci→label yang ditulis tangan di luar Mazhab.kt.
        val pola = Regex(""""(syafii|hanafi|maliki|hambali)"\s*to\s*"""")
        File(root(), "src/main/java/id/or/kemenag/yasintahlil").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "Mazhab.kt" }
            .forEach { f ->
                val isi = f.readText()
                pola.findAll(isi).forEach { pelanggar += "${f.name}: ${it.value}" }
            }
        assertTrue("Daftar mazhab ditulis ulang di luar Mazhab.kt: $pelanggar", pelanggar.isEmpty())
    }
}
