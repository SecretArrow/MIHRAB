package id.or.kemenag.yasintahlil.feature.almarhum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Patokan hitungan peringatan tahlil dipakukan di sini — tanggal-tanggalnya tampil kepada
 * pengguna sebagai hari yang dijanjikan, jadi hitungan yang bergeser sehari pun harus ketahuan
 * sebelum terbit. [AlmarhumHitung] sengaja tanpa Android: uji ini berjalan murni di JVM, dan
 * "hari ini" ditentukan lewat parameter (bukan jam dinding), sehingga ujinya tidak bergantung
 * pada tanggal mesin yang menjalankannya — pola `TasbihHarianTest`.
 */
class AlmarhumHitungTest {

    private val wafat = LocalDate.of(2026, 1, 1)

    @Test
    fun tambahHariMenghitungSejakHariWafat() {
        // Hari wafat sendiri adalah hari ke-0: hari ke-7 jatuh seminggu sesudahnya, bukan enam hari.
        assertEquals(LocalDate.of(2026, 1, 8), AlmarhumHitung.tambahHari(wafat, 7))
        assertEquals(LocalDate.of(2026, 2, 10), AlmarhumHitung.tambahHari(wafat, 40))
        assertEquals(LocalDate.of(2026, 4, 11), AlmarhumHitung.tambahHari(wafat, 100))
        // Seribu hari diverifikasi terhadap java.time sendiri, bukan diketik ulang: 2028 adalah
        // tahun kabisat, dan selisih sehari pada rentang dua-tahun lebih mudah lolos tanpa terlihat.
        assertEquals(wafat.plusDays(1000), AlmarhumHitung.tambahHari(wafat, 1000))
        assertEquals(LocalDate.of(2028, 9, 27), AlmarhumHitung.tambahHari(wafat, 1000))
    }

    @Test
    fun tambahHariLintasBulanDanTahun() {
        assertEquals(LocalDate.of(2026, 2, 1), AlmarhumHitung.tambahHari(LocalDate.of(2026, 1, 31), 1))
        assertEquals(LocalDate.of(2027, 1, 1), AlmarhumHitung.tambahHari(LocalDate.of(2026, 12, 31), 1))
        // 2028 kabisat: 29 Februari sah sebagai hari berikutnya setelah 28 Februari.
        assertEquals(LocalDate.of(2028, 2, 29), AlmarhumHitung.tambahHari(LocalDate.of(2028, 2, 28), 1))
    }

    @Test
    fun peringatanMencakupEmpatHariTradisiDanTigaHaul() {
        val semua = AlmarhumHitung.peringatan(wafat)
        assertEquals(7, semua.size)
        assertEquals(listOf(7L, 40L, 100L, 1000L), semua.mapNotNull { it.hariKe })
        assertEquals(
            listOf("Hari ke-7", "Hari ke-40", "Hari ke-100", "Hari ke-1000"),
            semua.filter { it.hariKe != null }.map { it.label },
        )
        assertEquals(
            listOf("Haul ke-1", "Haul ke-2", "Haul ke-3"),
            semua.filter { it.hariKe == null }.map { it.label },
        )
    }

    @Test
    fun haulDihitungPerTahunKalender() {
        val semua = AlmarhumHitung.peringatan(wafat)
        assertEquals(LocalDate.of(2027, 1, 1), semua.first { it.hariKe == null }.tanggal)
        assertEquals(LocalDate.of(2028, 1, 1), semua.last { it.label == "Haul ke-2" }.tanggal)
        // plusYears menangani wafat 29 Februari: haul pertamanya jatuh 28 Februari tahun berikutnya.
        assertEquals(
            LocalDate.of(2025, 2, 28),
            AlmarhumHitung.peringatan(LocalDate.of(2024, 2, 29)).first { it.label == "Haul ke-1" }.tanggal,
        )
    }

    @Test
    fun peringatanTerurutTanggalKarenaSeribuHariJatuhDiAntaraHaul() {
        // Hari ke-1000 (sekitar dua tahun sembilan bulan) jatuh setelah Haul ke-2 dan sebelum
        // Haul ke-3; urutan tetap (hari dulu baru haul) akan membuat "berikutnya" melompati haul
        // yang lebih dekat — karenanya daftarnya harus terurut menurut tanggalnya.
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 8), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 4, 11),
                LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1), LocalDate.of(2028, 9, 27),
                LocalDate.of(2029, 1, 1),
            ),
            AlmarhumHitung.peringatan(wafat).map { it.tanggal },
        )
    }

    @Test
    fun berikutnyaAdalahYangPertamaBelumLewat() {
        // Hari wafat itu sendiri: peringatan pertama adalah hari ke-7.
        assertEquals("Hari ke-7", AlmarhumHitung.berikutnya(wafat, LocalDate.of(2026, 1, 1))?.label)
        assertEquals("Hari ke-40", AlmarhumHitung.berikutnya(wafat, LocalDate.of(2026, 1, 9))?.label)
        // Semua hari tradisi telah lewat: haul-lah yang berikutnya.
        assertEquals("Haul ke-1", AlmarhumHitung.berikutnya(wafat, LocalDate.of(2026, 6, 1))?.label)
        // Antara Haul ke-2 dan ke-3, hari ke-1000 yang lebih dekat.
        assertEquals("Hari ke-1000", AlmarhumHitung.berikutnya(wafat, LocalDate.of(2028, 6, 1))?.label)
    }

    @Test
    fun berikutnyaPadaHariPeringatanAdalahPeringatanItu() {
        val hariKe40 = LocalDate.of(2026, 2, 10)
        val berikutnya = AlmarhumHitung.berikutnya(wafat, hariKe40)
        assertEquals("Hari ke-40", berikutnya?.label)
        assertEquals(hariKe40, berikutnya?.tanggal)
    }

    @Test
    fun berikutnyaNullSetelahSeluruhJadwalTerlewat() {
        // Wafat lebih dari tiga tahun lalu: 7/40/100/1000 dan tiga haul pertama semuanya telah lewat.
        assertNull(AlmarhumHitung.berikutnya(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun isoDariIsoBersalahBentukBaku() {
        assertEquals("2026-01-01", AlmarhumHitung.iso(wafat))
        // Tahun pendek tetap empat digit: bentuk ini isi kolom tanggal di Room, bukan tampilan.
        assertEquals("0999-12-31", AlmarhumHitung.iso(LocalDate.of(999, 12, 31)))
        assertEquals(wafat, AlmarhumHitung.dariIso(AlmarhumHitung.iso(wafat)))
    }

    @Test
    fun dariIsoMenolakBentukLainTanpaGagal() {
        assertNull(AlmarhumHitung.dariIso("2026-13-01")) // bulan 13 tidak ada
        assertNull(AlmarhumHitung.dariIso("2026-02-30")) // 30 Februari tidak ada
        assertNull(AlmarhumHitung.dariIso("abc"))
        assertNull(AlmarhumHitung.dariIso("")) // belum diisi bukan galat — null memang maknanya
        // Bentuk longgar ditolak — teks ini kunci tabel, dan bentuk ganda membuat satu tanggal
        // menjadi dua baris.
        assertNull(AlmarhumHitung.dariIso("2026-1-1"))
        assertNull(AlmarhumHitung.dariIso("2026/01/01"))
    }

    @Test
    fun tanggalIndoMemakaiNamaBulanIndonesia() {
        assertEquals("17 Agustus 2025", AlmarhumHitung.tanggalIndo(LocalDate.of(2025, 8, 17)))
        assertEquals("1 Januari 2026", AlmarhumHitung.tanggalIndo(LocalDate.of(2026, 1, 1)))
        // "Agustus" — nama bulan Indonesia, bukan "August" milik Inggris.
        assertEquals("Agustus", AlmarhumHitung.bulan[7])
    }

    @Test
    fun bulanMenutupDuaBelasBulan() {
        assertEquals(12, AlmarhumHitung.bulan.size)
        assertEquals(12, AlmarhumHitung.bulan.toSet().size)
    }
}
