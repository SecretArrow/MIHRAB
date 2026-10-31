package id.or.kemenag.yasintahlil.feature.tahlil

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Aturan Daftar Arwah ([DaftarArwah]) — nama arwah adalah **data pengguna**, bukan teks
 * bundel: buku "Tahlil dan doa IMAM" menulis titik-titik pada Bagian 4 sebagai tempat imam
 * menyebut nama arwah yang dituju, dan aplikasi hanya menyediakan tempatnya tanpa pernah
 * mengubah lafaz bacaan. Aturan pembersihannya karena itu wajib bisa diandalkan di JVM:
 * baris baru di dalam nama akan merusak format simpanan satu-nama-per-baris, duplikat
 * membingungkan pembacaan, dan daftar tak berbatas membanjiri kartu.
 */
class DaftarArwahTest {

    @Test
    fun parseMembuangBarisKosongDanMerapikanSpasi() {
        assertEquals(listOf("H. Ahmad", "Hj. Fatimah"),
            DaftarArwah.parse("\n  H.   Ahmad \n\nHj. Fatimah\n"))
        assertEquals(emptyList<String>(), DaftarArwah.parse(""))
        assertEquals(emptyList<String>(), DaftarArwah.parse("   \n  \n"))
    }

    @Test
    fun namaDenganBarisBaruDirapikanMenjadiSatuBaris() {
        // Baris baru di dalam nama merusak format simpanan — wajib menjadi satu baris.
        assertEquals(listOf("Ahmad Sulaiman"), DaftarArwah.parse("Ahmad\nSulaiman"))
        assertEquals(listOf("Ahmad Sulaiman"), DaftarArwah.tambah(emptyList(), "Ahmad\r\nSulaiman"))
    }

    @Test
    fun namaDipangkasPadaBatasPanjangnya() {
        val panjang = "A".repeat(DaftarArwah.MAKS_PANJANG + 25)
        assertEquals(DaftarArwah.MAKS_PANJANG, DaftarArwah.bersih(panjang).length)
    }

    @Test
    fun duplikatTanpaPandangBesarKecilHurufTidakDitambahDuaKali() {
        val lama = listOf("Ahmad")
        assertEquals(lama, DaftarArwah.tambah(lama, "ahmad"))
        assertEquals(lama, DaftarArwah.tambah(lama, "  AHMAD "))
        assertEquals(listOf("Ahmad", "Muhammad"), DaftarArwah.tambah(lama, "Muhammad"))
    }

    @Test
    fun jumlahEntriTidakMelebihiBatasnya() {
        val penuh = (1..DaftarArwah.MAKS_ENTRI + 10).map { "Nama $it" }
        val hasil = DaftarArwah.parse(penuh.joinToString("\n"))
        assertEquals(DaftarArwah.MAKS_ENTRI, hasil.size)
        // Nama yang lebih dulu masuk dipertahankan — daftar tidak menggusur yang lama diam-diam.
        assertEquals("Nama 1", hasil.first())
    }

    @Test
    fun roundTripSerializeParseSamaDenganAwalnya() {
        val awal = listOf("H. Ahmad", "Hj. Fatimah binti H. Ahmad", "Kyai Abdul Karim")
        assertEquals(awal, DaftarArwah.parse(DaftarArwah.serialize(awal)))
    }

    @Test
    fun hapusPadaIndeksSahDanTidakSah() {
        val lama = listOf("A", "B", "C")
        assertEquals(listOf("A", "C"), DaftarArwah.hapus(lama, 1))
        assertEquals(lama, DaftarArwah.hapus(lama, -1))
        assertEquals(lama, DaftarArwah.hapus(lama, 99))
        assertEquals(emptyList<String>(), DaftarArwah.hapus(emptyList(), 0))
    }

    @Test
    fun barisRingkasMemotongDenganPenandaSisa() {
        val enam = (1..6).map { "Nama$it" }
        assertEquals(enam.joinToString(" • "), DaftarArwah.ringkas(enam, maksTampil = 6))
        val lima = (1..5).map { "Nama$it" }
        assertEquals("Nama1 • Nama2 • … (+3)", DaftarArwah.ringkas(lima, maksTampil = 2))
        assertEquals("", DaftarArwah.ringkas(emptyList()))
    }
}
