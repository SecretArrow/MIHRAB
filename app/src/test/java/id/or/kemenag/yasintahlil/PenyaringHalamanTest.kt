package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"UI/UX dan Navigasi" meminta *"Sediakan pencarian dan akses cepat pada halaman yang
 * memiliki banyak konten"*.
 *
 * Dua halaman terpadat — Pustaka Dzikir (31 entri dalam 5 kelompok) dan Panduan Sholat (puluhan
 * entri) — dahulu menjawabnya dengan kotak penyaring sendiri di tiap tab. Tab-tab itu kini
 * dibubarkan (permintaan pengguna, 2026-10-15: tampilan setelah menu dibuka harus seperti menu
 * Ibadah, bukan tabs), sehingga kotak penyaring per-tab ikut hilang bersama tabnya — dan uji ini
 * menjaga penggantinya:
 *
 * 1. **Pencarian terpadu** kini satu-satunya jalan mencari; ia menelusuri seluruh entri dzikir
 *    lintas kelompok dan seluruh daftar + bacaan sholat — lebih luas daripada penyaring per-tab
 *    yang hanya melihat satu kelompok/tab pada satu waktu.
 * 2. **Akses cepat** dijaga bentuk menunya: kelompok dzikir menjadi baris menu, sholat wajib dan
 *    sunnah dikelompokkan pada satu halaman — tidak ada lagi isi yang tersembunyi di balik tab.
 * 3. Keadaan kosong tetap dijelaskan: kelompok dzikir yang tak dikenal (id-nya bisa hilang bila
 *    bundel konten diganti) diberitahukan, bukan dibiarkan sebagai layar buntu.
 */
class PenyaringHalamanTest {

    private fun sumber(relatif: String): String {
        val root = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
        return File(root, relatif).readText()
    }

    private fun layar(paket: String, nama: String): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/$paket/$nama.kt")

    /** Pencarian terpadu menutup kedua pustaka itu: dzikir dan sholat masuk indeksnya. */
    @Test
    fun pencarianTerpaduMenutupDzikirDanSholat() {
        val s = layar("search", "SearchScreen")
        assertTrue("indeks Pencarian tidak membaca pustaka dzikir", s.contains("repo.dzikir()"))
        assertTrue("indeks Pencarian tidak membaca daftar sholat", s.contains("repo.sholatWajib()"))
        assertTrue("indeks Pencarian tidak membaca bacaan sholat", s.contains("repo.sholatBacaan()"))
        // Hasil dzikir membuka kelompoknya langsung, bukan sekadar menu induknya.
        assertTrue("hasil dzikir tidak membuka kelompoknya",
            s.contains("Routes.dzikirGrup(grup.id)"))
    }

    /**
     * Tab yang dibubarkan tidak boleh tersembunyi isinya: keduanya kini menu terbuka.
     *
     * Menu Dzikir memakai baris menu bersama ([MenuBaris]) — bentuk yang sama dengan menu
     * Ibadah — dan Panduan Sholat menyusun ketiga bagiannya pada satu halaman yang digulir.
     */
    @Test
    fun bekasTabKiniMenuTerbukaTanpaTab() {
        val dzikir = layar("dzikir", "DzikirScreen")
        assertTrue("menu Dzikir tidak memakai baris menu bersama", dzikir.contains("MenuBaris("))
        assertFalse("menu Dzikir masih memakai tab", dzikir.contains("TabRow"))

        val sholat = layar("sholat", "SholatScreen")
        assertTrue("menu Sholat tidak memakai baris menu bersama", sholat.contains("MenuBaris("))
        assertFalse("menu Sholat masih memakai tab", sholat.contains("TabRow"))
        // Ketiga bekas tabnya tetap utuh sebagai bagian: wajib, sunnah, bacaan.
        listOf("Sholat wajib", "Sholat sunnah", "Bacaan sholat").forEach { bagian ->
            assertTrue("bagian \"$bagian\" hilang dari menu Sholat", sholat.contains(bagian))
        }
    }

    /** Kelompok dzikir yang tak dikenal harus dijelaskan, bukan layar buntu tanpa sebab. */
    @Test
    fun kelompokDzikirTakDikenalDijelaskan() {
        val s = layar("dzikir", "DzikirGroupScreen")
        assertTrue("layar kelompok dzikir tidak menampilkan keadaan kosong",
            s.contains("EmptyState("))
    }
}
