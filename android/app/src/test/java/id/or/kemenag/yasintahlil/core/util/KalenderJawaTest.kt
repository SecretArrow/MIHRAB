package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Uji [KalenderJawa] terhadap **tanggal jangkar nyata** — setiap rumus penanggalan di sini
 * berdiri di atas JDN, maka JDN-lah yang pertama dipaku ke jangkar yang dapat diperiksa siapa
 * pun: epoch Unix (1970-01-01), J2000.0 (2000-01-01), 1 Januari 1900, dan hari proklamasi
 * (17 Agustus 1945).
 *
 * Nilai jdn(17-08-1945) sempat tertulis 2431771 di draf spesifikasi — angka itu **tidak lolos**
 * tiga pemeriksaan independen: (1) 2451545 − 19860 hari (jarak ke J2000.0) = 2431685;
 * (2) 2440588 − 8903 hari (jarak ke epoch Unix) = 2431685; (3) jangkar weton proklamasi
 * **Jumat Legi** menuntut jdn mod 5 == 0 (Legi) dan mod 7 == 4 (Jumat) — terpenuhi oleh
 * 2431685, sedangkan 2431771 memberi mod 5 == 1 (Pahing) yang menyalahi sejarah. Karena itu
 * uji ini memaku 2431685, dan [KalenderJawa.pasaran] yang diturunkan darinya.
 */
class KalenderJawaTest {

    // ---- JDN: jangkar baku ----------------------------------------------------------------------

    @Test
    fun jdnJangkarBaku() {
        assertEquals(2440588L, KalenderJawa.jdn(LocalDate.of(1970, 1, 1)))  // epoch Unix
        assertEquals(2451545L, KalenderJawa.jdn(LocalDate.of(2000, 1, 1)))  // J2000.0
        assertEquals(2415021L, KalenderJawa.jdn(LocalDate.of(1900, 1, 1)))  // 100 th sebelum epoch
    }

    @Test
    fun jdnHariProklamasi() {
        // 17 Agustus 1945 = 2431685; tiga jalur verifikasinya ditulis di KDoc objek ini dan
        // diuji eksplisit oleh dua uji berikutnya.
        assertEquals(2431685L, KalenderJawa.jdn(LocalDate.of(1945, 8, 17)))
    }

    @Test
    fun jdnProklamasiSesuaiJarakKeJangkarBaku() {
        val proklamasi = LocalDate.of(1945, 8, 17)
        val keJ2000 = LocalDate.of(2000, 1, 1).toEpochDay() - proklamasi.toEpochDay()
        val keEpoch = LocalDate.of(1970, 1, 1).toEpochDay() - proklamasi.toEpochDay()
        assertEquals(2451545L, KalenderJawa.jdn(proklamasi) + keJ2000)  // 19860 hari
        assertEquals(2440588L, KalenderJawa.jdn(proklamasi) + keEpoch)  // 8903 hari
    }

    /**
     * Oracle independen: java.time menghitung sendiri hari sejak 1970-01-01 (`toEpochDay`),
     * dengan implementasi yang berbeda dari rumus integer klasik di [KalenderJawa.jdn]. Sepanjang
     * rentang 1899–2101 keduanya wajib berhimpit — termasuk ujung tahun, tanggal 29 Februari,
     * dan awal bulan yang menempuh jalur Januari/Februari pada rumus.
     */
    @Test
    fun jdnBerhimpitDenganHitunganHariJavaTime() {
        var t = LocalDate.of(1899, 12, 20)
        val akhir = LocalDate.of(2101, 1, 10)
        while (t.isBefore(akhir)) {
            assertEquals("jdn melencet di $t", t.toEpochDay() + 2440588L, KalenderJawa.jdn(t))
            t = t.plusMonths(1).plusDays(3)
        }
    }

    /** Jalur Januari/Februari (pembagian `(14 − m) / 12`): 1 Jan dan 1 Mar beberapa tahun. */
    @Test
    fun jdnJalurJanuariFebruari() {
        listOf(1900, 1945, 1970, 2000, 2024, 2026).forEach { tahun ->
            val jan = KalenderJawa.jdn(LocalDate.of(tahun, 1, 1))
            val mar = KalenderJawa.jdn(LocalDate.of(tahun, 3, 1))
            assertEquals("1 Jan → 1 Mar $tahun", if (LocalDate.of(tahun, 1, 1).isLeapYear) 60L else 59L, mar - jan)
        }
    }

    // ---- Pasaran & weton: jangkar sejarah -------------------------------------------------------

    @Test
    fun pasaranJangkarProklamasiDanSeratusTahunSebelumnya() {
        // Weton proklamasi yang masyhur: Jumat Legi, 17 Agustus 1945.
        assertEquals("Legi", KalenderJawa.pasaran(LocalDate.of(1945, 8, 17)))
        // Jangkar kedua: Senin Pahing, 1 Januari 1900 — terpisah 16664 hari dari proklamasi
        // (16664 mod 5 = 4: Pahing + 4 = Legi, siklusnya konsisten).
        assertEquals("Pahing", KalenderJawa.pasaran(LocalDate.of(1900, 1, 1)))
    }

    @Test
    fun wetonJangkarSejarah() {
        assertEquals("Jumat Legi", KalenderJawa.weton(LocalDate.of(1945, 8, 17)))
        assertEquals("Senin Pahing", KalenderJawa.weton(LocalDate.of(1900, 1, 1)))
        // Hari Pahlawan 10 November 1945 = Sabtu Legi — dipakai sebagai contoh turunan kedua:
        // 85 hari setelah proklamasi (85 habis dibagi 5 → pasaran tetap Legi, hari bergeser 1).
        assertEquals("Sabtu Legi", KalenderJawa.weton(LocalDate.of(1945, 11, 10)))
    }

    @Test
    fun pasaranBerputarLimaHari() {
        val t = LocalDate.of(2026, 6, 15)
        assertEquals("siklus pasaran 5 hari", KalenderJawa.pasaran(t), KalenderJawa.pasaran(t.plusDays(5)))
        assertNotEquals("pasaran hari berikutnya harus berganti", KalenderJawa.pasaran(t), KalenderJawa.pasaran(t.plusDays(1)))
        // Seluruh pasaran tersambung tanpa terputus: 7 hari berikutnya memuat 5 nama berbeda.
        val sepekan = (0L..6L).map { KalenderJawa.pasaran(t.plusDays(it)) }
        assertEquals(5, sepekan.toSet().size)
        assertTrue(sepekan.all { it in KalenderJawa.PASARAN })
    }

    // ---- Neptu -----------------------------------------------------------------------------------

    @Test
    fun neptuJangkarPrimbon() {
        // Jumat 6 + Legi 5 = 11 (weton proklamasi); Senin 4 + Pahing 9 = 13.
        assertEquals(11, KalenderJawa.neptu(LocalDate.of(1945, 8, 17)))
        assertEquals(13, KalenderJawa.neptu(LocalDate.of(1900, 1, 1)))
    }

    // ---- Nama hari & tanggal ----------------------------------------------------------------------

    @Test
    fun namaHariMengikutiDayOfWeek() {
        // 1 Januari 2026 jatuh pada Kamis — dikonversi lewat nama hari milik KalenderJawa.
        assertEquals("Kamis", KalenderJawa.hari(LocalDate.of(2026, 1, 1)))
        // Satu pekan penuh dari hari Minggu memetakan tujuh nama hari tanpa tertukar.
        val minggu = LocalDate.of(2026, 1, 4) // Minggu
        listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
            .forEachIndexed { i, nama -> assertEquals(nama, KalenderJawa.hari(minggu.plusDays(i.toLong()))) }
    }

    @Test
    fun tanggalIndoProklamasi() {
        assertEquals("17 Agustus 1945", KalenderJawa.tanggalIndo(LocalDate.of(1945, 8, 17)))
        assertEquals("1 Januari 1900", KalenderJawa.tanggalIndo(LocalDate.of(1900, 1, 1)))
    }

    // ---- Bulan & tahun Jawa ----------------------------------------------------------------------

    @Test
    fun bulanJawaSelarasUrutanBulanHijriah() {
        assertEquals("Sura", KalenderJawa.bulanJawa(1))
        assertEquals("Mulud", KalenderJawa.bulanJawa(3))
        assertEquals("Pasa", KalenderJawa.bulanJawa(9))
        assertEquals("Besar", KalenderJawa.bulanJawa(12))
        assertEquals(12, KalenderJawa.BULAN_JAWA.size)
        assertEquals(12, KalenderJawa.BULAN_MASEHI.size)
    }

    @Test
    fun tahunJawaKonvensiAjiSaka() {
        assertEquals(1370, KalenderJawa.tahunJawa(1448))
    }
}
