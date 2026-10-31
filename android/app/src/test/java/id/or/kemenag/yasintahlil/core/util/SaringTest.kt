package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penyaring di layar (`plan.txt`: *"Sediakan pencarian dan akses cepat pada halaman yang memiliki
 * banyak konten"*).
 *
 * Aturan yang paling mudah salah adalah kata kunci **kosong**: untuk pencarian terpadu kata kosong
 * berarti tidak ada hasil, tetapi untuk penyaring di layar ia harus berarti *semua* entri — daftar
 * yang sedang dibaca tidak boleh mendadak kosong hanya karena kotak pencariannya belum diisi.
 */
class SaringTest {

    private val item = listOf(
        Triple("Doa masuk masjid", "اللَّهُمَّ افْتَحْ", "Allahummaftah"),
        Triple("Doa keluar rumah", "بِسْمِ اللَّهِ", "Bismillah"),
        Triple("Doa sebelum tidur", "بِاسْمِكَ", "Bismika"),
    )

    private fun teks(t: Triple<String, String, String>) = listOf(t.first, t.second, t.third)

    private fun tapis(kata: String) = Saring.tapis(kata, item) { teks(it) }.map { it.first }

    @Test
    fun kataKosongMenampilkanSemua() {
        assertEquals(item.map { it.first }, tapis(""))
        assertEquals(item.map { it.first }, tapis("   "))
    }

    @Test
    fun mencocokkanJudul() {
        assertEquals(listOf("Doa masuk masjid"), tapis("masuk"))
    }

    /** Pencocokan juga menyentuh teks Arab dan transliterasi, bukan hanya judul berbahasa Indonesia. */
    @Test
    fun mencocokkanArabDanTransliterasi() {
        assertEquals(listOf("Doa keluar rumah"), tapis("Bismillah"))
        assertEquals(listOf("Doa sebelum tidur"), tapis("بِاسْمِكَ"))
    }

    @Test
    fun tidakBedaHurufBesarKecil() {
        assertEquals(listOf("Doa masuk masjid"), tapis("DOA MASUK"))
    }

    @Test
    fun kataTanpaHasilMengembalikanDaftarKosong() {
        assertTrue(tapis("tidak ada entri seperti ini").isEmpty())
    }

    @Test
    fun urutanHasilMengikutiUrutanAsal() {
        assertEquals(listOf("Doa masuk masjid", "Doa keluar rumah", "Doa sebelum tidur"), tapis("doa"))
    }

    @Test
    fun cocokMenghormatiSpasiDiTepiKataKunci() {
        assertTrue(Saring.cocok("  masuk  ", listOf("Doa masuk masjid")))
        assertFalse(Saring.cocok("keluar", listOf("Doa masuk masjid")))
    }
}
