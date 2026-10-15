package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Uji [PuasaSunnah] — aturan tanggalnya dipaku ke tanggal Hijriah nyata: label mingguan
 * (Senin/Kamis) mengikuti nomor hari, label tanggalan (Ayyamul Bidh, Tasu'a, Asyura, Arafah,
 * Syawal) mengikuti hari/bulan Hijriah, dan hari biasa tidak mengarang peringatan.
 */
class PuasaSunnahTest {

    /** Nomor hari dalam minggu mengikuti DayOfWeek.getValue: 1 = Senin … 7 = Minggu. */
    private val senin = DayOfWeek.MONDAY.value
    private val selasa = DayOfWeek.TUESDAY.value
    private val rabu = DayOfWeek.WEDNESDAY.value
    private val kamis = DayOfWeek.THURSDAY.value
    private val jumat = DayOfWeek.FRIDAY.value
    private val minggu = DayOfWeek.SUNDAY.value

    @Test
    fun seninKamisMengikutiHariDalamMinggu() {
        // Tanggal biasa (3 Jumadilakhir) — satu-satunya penanda adalah hari dalam minggunya.
        assertEquals(listOf("Puasa Senin"), PuasaSunnah.puasaSunnah(3, 5, senin))
        assertEquals(listOf("Puasa Kamis"), PuasaSunnah.puasaSunnah(3, 5, kamis))
        // Hari lain tidak berlabel mingguan.
        assertTrue(PuasaSunnah.puasaSunnah(3, 5, selasa).isEmpty())
        assertTrue(PuasaSunnah.puasaSunnah(3, 5, rabu).isEmpty())
        assertTrue(PuasaSunnah.puasaSunnah(3, 5, jumat).isEmpty())
        assertTrue(PuasaSunnah.puasaSunnah(3, 5, minggu).isEmpty())
    }

    @Test
    fun ayyamulBidhSetiapBulan() {
        // 13–15 tiap bulan Hijriah apa pun; Rabu dipilih supaya label mingguannya kosong.
        assertEquals(
            listOf("Puasa Ayyamul Bidh"),
            PuasaSunnah.puasaSunnah(13, 3, rabu),
        )
        assertEquals(
            listOf("Puasa Ayyamul Bidh"),
            PuasaSunnah.puasaSunnah(14, 8, minggu),
        )
        assertEquals(
            listOf("Puasa Ayyamul Bidh"),
            PuasaSunnah.puasaSunnah(15, 2, selasa),
        )
        // Bulan 10 (Syawal) ikut mendapatnya — bersama rangkaian Syawal (lihat uji di bawah).
        (1..12).forEach { bulan ->
            assertTrue(
                "13 bulan $bulan seharusnya Ayyamul Bidh",
                PuasaSunnah.puasaSunnah(13, bulan, rabu).contains("Puasa Ayyamul Bidh"),
            )
        }
    }

    @Test
    fun tasuAsyuraDanArafah() {
        assertEquals(listOf("Puasa Tasu'a"), PuasaSunnah.puasaSunnah(9, 1, minggu))
        assertEquals(listOf("Puasa Asyura"), PuasaSunnah.puasaSunnah(10, 1, rabu))
        assertEquals(listOf("Puasa Arafah"), PuasaSunnah.puasaSunnah(9, 12, jumat))
        // 9/10 bulan lain bukan Tasu'a/Asyura/Arafah.
        assertTrue(PuasaSunnah.puasaSunnah(10, 2, minggu).isEmpty())
        assertTrue(PuasaSunnah.puasaSunnah(9, 3, minggu).isEmpty())
    }

    @Test
    fun rangkaianSyawal() {
        assertEquals(listOf("Rangkaian puasa Syawal"), PuasaSunnah.puasaSunnah(5, 10, minggu))
        // Tanggal 1 Syawal adalah Idul Fitri — hari yang haram berpuasa, tidak ikut dirangkaikan.
        assertTrue(PuasaSunnah.puasaSunnah(1, 10, minggu).isEmpty())
    }

    @Test
    fun satuHariBolehMemuatBanyakLabel() {
        // Senin yang jatuh 13 Syawal: puasa mingguan, bidh, dan rangkaian Syawal sekaligus —
        // urutnya tetap: mingguan dulu, baru tanggalan.
        assertEquals(
            listOf("Puasa Senin", "Puasa Ayyamul Bidh", "Rangkaian puasa Syawal"),
            PuasaSunnah.puasaSunnah(13, 10, senin),
        )
    }

    @Test
    fun hariBesarIslam() {
        assertEquals("Tahun Baru Hijriah", PuasaSunnah.hariBesar(1, 1))
        assertEquals("Hari Asyura", PuasaSunnah.hariBesar(10, 1))
        assertEquals("Maulid Nabi", PuasaSunnah.hariBesar(12, 3))
        assertEquals("Isra Mikraj", PuasaSunnah.hariBesar(27, 7))
        assertEquals("Nisfu Syakban", PuasaSunnah.hariBesar(15, 8))
        assertEquals("Awal Ramadan", PuasaSunnah.hariBesar(1, 9))
        assertEquals("Idul Fitri", PuasaSunnah.hariBesar(1, 10))
        assertEquals("Idul Adha", PuasaSunnah.hariBesar(10, 12))
        assertNull(PuasaSunnah.hariBesar(11, 5))
    }

    @Test
    fun hariBiasaTanpaPeringatan() {
        // 11 Jumadilakhir, Selasa: bukan Senin/Kamis, bukan 13–15, bukan tanggal khusus mana pun.
        assertTrue(PuasaSunnah.puasaSunnah(11, 5, selasa).isEmpty())
        assertNull(PuasaSunnah.hariBesar(11, 5))
    }
}
