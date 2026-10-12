package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"UI/UX dan Navigasi" meminta *"Sediakan pencarian dan akses cepat pada halaman yang
 * memiliki banyak konten"*.
 *
 * Pencarian terpadu sudah ada di menu Pencarian, tetapi ia menjawab pertanyaan lain ("di mana
 * bacaan X di seluruh aplikasi") dan berpindah layar. Dua halaman terpadat — Pustaka Dzikir (31
 * entri dalam 5 kelompok) dan Panduan Sholat (puluhan entri dalam 3 tab) — sebelumnya hanya bisa
 * ditelusuri dengan menggulir dan berpindah tab. Uji ini menjaga agar penyaring di layar tidak
 * hilang lagi.
 */
class PenyaringHalamanTest {

    private fun sumber(relatif: String): String {
        val root = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
        return File(root, relatif).readText()
    }

    private fun layar(paket: String, nama: String): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/$paket/$nama.kt")

    @Test
    fun halamanDzikirPunyaPenyaringSendiri() {
        val s = layar("dzikir", "DzikirScreen")
        assertTrue("DzikirScreen tidak menyaring daftarnya", s.contains("Saring.tapis("))
        assertTrue("DzikirScreen tidak punya kotak pencarian", s.contains("OutlinedTextField("))
    }

    @Test
    fun halamanSholatPunyaPenyaringSendiri() {
        val s = layar("sholat", "SholatScreen")
        assertTrue("SholatScreen tidak menyaring daftarnya", s.contains("Saring.tapis("))
        assertTrue("SholatScreen tidak punya kotak pencarian", s.contains("OutlinedTextField("))
    }

    /** Kata kunci yang tak menghasilkan apa pun harus dijelaskan, bukan layar kosong tanpa sebab. */
    @Test
    fun hasilKosongDijelaskanDanDapatDikosongkan() {
        listOf(layar("dzikir", "DzikirScreen"), layar("sholat", "SholatScreen")).forEach { s ->
            assertTrue("layar tidak menampilkan keadaan kosong saat penyaring tak menemukan apa pun",
                s.contains("EmptyState("))
        }
    }
}
