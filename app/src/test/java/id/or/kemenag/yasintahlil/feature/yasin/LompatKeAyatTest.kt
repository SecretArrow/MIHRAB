package id.or.kemenag.yasintahlil.feature.yasin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Yasin": *"Bookmark ayat."* — tombol "Ke ayat", tombol "Lanjut ayat N", dan daftar
 * bookmark semuanya mengarahkan daftar ke sebuah ayat.
 *
 * Sebelumnya ketiganya memakai indeks `n - 1`, yang hanya benar bila tak ada pencarian aktif:
 * daftar yang digulir adalah hasil penyaringan, sehingga dengan pencarian aktif ayat ke-n bukan lagi
 * item ke-(n-1) dan pengguna mendarat di ayat yang salah tanpa diberi tahu. Sekarang nomor ayat
 * dicari pada daftar yang **sedang tampil**.
 */
class LompatKeAyatTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private val layar =
        "src/main/java/id/or/kemenag/yasintahlil/feature/yasin/YasinScreen.kt"

    private fun sumber(): String = File(root(), layar).readText()

    @Test
    fun indeksDihitungDariDaftarYangTampil() {
        val s = sumber()
        assertTrue("nomor ayat tidak dicari pada daftar yang sedang tampil",
            s.contains("indexOfFirst { it.nomor == n }"))
        val gulir = s.lines().filter { it.contains("animateScrollToItem(") }
        assertTrue("tidak ada gulir ke ayat", gulir.isNotEmpty())
        assertTrue("gulir memakai indeks yang bukan hasil pencarian ayat: $gulir",
            gulir.all { it.contains("(idx)") })
    }

    /** Indeks `n - 1` yang lama tidak boleh kembali: ia mengabaikan penyaring pencarian. */
    @Test
    fun tidakAdaLagiIndeksMentah() {
        val baris = sumber().lines().filter { it.contains("animateScrollToItem(") }
        assertFalse("gulir masih menghitung indeks mentah: $baris",
            baris.any { it.contains("- 1") || it.contains("n - 1") })
    }

    @Test
    fun ketigaJalurLompatLewatSatuFungsi() {
        val s = sumber()
        assertTrue("tidak ada fungsi lompat bersama", s.contains("fun lompatKe("))
        assertTrue("tombol \"Ke ayat\" tidak lewat fungsi lompat", s.contains("lompatKe(it, tandai = true)"))
        assertTrue("tombol lanjut tidak lewat fungsi lompat", s.contains("lompatKe(s.mark)"))
        assertTrue("daftar bookmark tidak lewat fungsi lompat", s.contains("lompatKe(n)"))
    }

    @Test
    fun nomorDiLuarSuratTidakDitebak() {
        val s = sumber()
        assertTrue("tidak ada batas nomor ayat", s.contains("if (n !in 1..surah.ayatCount) return"))
        assertTrue("ayat tersaring pencarian tidak membuka penyaringnya",
            s.contains("query.isNotBlank() -> query = \"\""))
    }
}
