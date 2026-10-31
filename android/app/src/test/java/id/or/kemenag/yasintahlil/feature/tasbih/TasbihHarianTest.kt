package id.or.kemenag.yasintahlil.feature.tasbih

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Uji penolong tanggal Tasbih Digital. [TasbihHarian] sengaja murni (tanpa Android) supaya aturan
 * labelnya dapat diuji di JVM — "hari ini" dan "kemarin" ditentukan lewat parameter
 * [TasbihHarian.label] (bukan jam dinding), sehingga ujinya tidak bergantung pada tanggal di
 * mesin yang menjalankannya.
 */
class TasbihHarianTest {

    @Test
    fun isoSelaluEmpatDigitTahunDuaDigitSisa() {
        assertEquals("2025-10-16", TasbihHarian.iso(LocalDate.of(2025, 10, 16)))
        assertEquals("2025-01-05", TasbihHarian.iso(LocalDate.of(2025, 1, 5)))
        // Tahun pendek tetap empat digit: bentuk ini kunci tabel Room dan pembanding string,
        // bukan sekadar tampilan.
        assertEquals("0999-12-31", TasbihHarian.iso(LocalDate.of(999, 12, 31)))
    }

    @Test
    fun labelHariIniUntukTanggalYangSama() {
        assertEquals("Hari ini", TasbihHarian.label("2025-10-16", "2025-10-16"))
    }

    @Test
    fun labelKemarinHanyaUntukTepatSehariSebelumnya() {
        assertEquals("Kemarin", TasbihHarian.label("2025-10-15", "2025-10-16"))
        // Dua hari sebelumnya bukan "Kemarin" — jatuh ke bentuk tanggal-bulan.
        assertEquals("14 Okt", TasbihHarian.label("2025-10-14", "2025-10-16"))
    }

    @Test
    fun labelTanggalLainMemakaiBentukTanggalBulanPendek() {
        assertEquals("15 Okt", TasbihHarian.label("2025-10-15", "2025-10-20"))
        assertEquals("3 Feb", TasbihHarian.label("2025-02-03", "2025-02-10"))
        // "Agu" — singkatan Indonesia, bukan "Aug" milik Inggris.
        assertEquals("1 Agu", TasbihHarian.label("2025-08-01", "2025-08-20"))
    }

    /** Bentuk simpan dan label memakai jalur yang sama, bukan dua ejaan yang bisa menyimpang. */
    @Test
    fun labelMembacaBentukDariIso() {
        val hariIni = TasbihHarian.iso(LocalDate.of(2025, 12, 31))
        assertEquals("Hari ini", TasbihHarian.label(hariIni, hariIni))
        assertEquals("Kemarin", TasbihHarian.label(TasbihHarian.iso(LocalDate.of(2025, 12, 30)), hariIni))
        assertEquals("24 Des", TasbihHarian.label(TasbihHarian.iso(LocalDate.of(2025, 12, 24)), hariIni))
    }

    /**
     * Tanggal yang tidak terbaca dikembalikan apa adanya — baris rusak tetap tampil di riwayat,
     * hanya tanpa label ramahnya, bukan membuat layar gagal.
     */
    @Test
    fun labelTanggalTakTerbacaDikembalikanApaAdanya() {
        assertEquals("bukan-tanggal", TasbihHarian.label("bukan-tanggal", "2025-10-16"))
    }

    /** Dua belas singkatan unik — satu per bulan, tanpa bulan yang tak terpetakan. */
    @Test
    fun bulanPendekMenutupDuaBelasBulan() {
        assertEquals(12, TasbihHarian.bulanPendek.size)
        assertEquals(12, TasbihHarian.bulanPendek.toSet().size)
    }
}
