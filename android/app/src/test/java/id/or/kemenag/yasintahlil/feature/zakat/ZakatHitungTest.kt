package id.or.kemenag.yasintahlil.feature.zakat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Patokan hitungan zakat dipakukan di sini — angka-angkanya tampil kepada pengguna sebagai
 * nominal rupiah yang dituntut, jadi kadar atau takaran yang bergeser sedikit pun harus
 * ketahuan sebelum terbit. `ZakatHitung` sengaja tanpa Android: uji ini berjalan murni di JVM.
 */
class ZakatHitungTest {

    @Test
    fun fitrahMengalikanJiwaTakaranDanHargaBeras() {
        // 4 jiwa × 2,5 kg × Rp 13.000
        assertEquals(130000.0, ZakatHitung.fitrah(4, 13000.0), 0.0)
    }

    @Test
    fun takaranBerasFitrahSeparuhKgPadaJiwaGanjil() {
        assertEquals(10.0, ZakatHitung.fitrahKg(4), 0.0)
        assertEquals(2.5, ZakatHitung.fitrahKg(1), 0.0)
    }

    @Test
    fun zakatMalDuaSetengahPersen() {
        assertEquals(25000.0, ZakatHitung.mal(1000000.0), 0.0)
    }

    @Test
    fun nisabMalSenilaiDelapanPuluhLimaGramEmas() {
        assertEquals(8500000.0, ZakatHitung.nisabEmas(100000.0), 0.0)
    }

    @Test
    fun zakatPenghasilanDuaSetengahPersenPerBulan() {
        assertEquals(125000.0, ZakatHitung.penghasilan(5000000.0), 0.0)
    }

    @Test
    fun rupiahMemakaiTitikPemisahRibuan() {
        assertEquals("Rp 1.250.000", ZakatHitung.rupiah(1250000.0))
    }

    @Test
    fun rupiahNolUntukTanpaNilai() {
        assertEquals("Rp 0", ZakatHitung.rupiah(0.0))
        assertEquals("Rp 0", ZakatHitung.rupiah(-5.0))
    }

    @Test
    fun beratBerasSatuDesimalBerkoma() {
        assertEquals("10 kg", ZakatHitung.kg(10.0))
        assertEquals("6,3 kg", ZakatHitung.kg(6.3))
        assertEquals("0 kg", ZakatHitung.kg(0.0))
    }
}
