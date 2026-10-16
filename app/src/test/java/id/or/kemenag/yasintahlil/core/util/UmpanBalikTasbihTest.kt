package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penjaga kosakata mode umpan balik ketukan Tasbih ([UmpanBalikTasbih]).
 *
 * Mode ini dipakai tiga pihak yang tidak saling melihat: Pengaturan menuliskannya ke DataStore,
 * TasbihViewModel membaca dan membekannya, layar Tasbih melaksanakannya. Salah satu saja yang
 * mengeja nilainya berbeda (mis. `"beep"` vs `"bunyi"`) membuat pilihan yang tersimpan diam-diam
 * tidak pernah dilaksanakan — pindah mode terasa mati. Uji ini mengunci kuncinya, urutan
 * tampilannya, perilaku penjepitnya, dan labelnya di satu tempat.
 */
class UmpanBalikTasbihTest {

    @Test
    fun daftarModeLengkapDanBawaanGetar() {
        assertEquals(listOf("diam", "getar", "beep"), UmpanBalikTasbih.SEMUA)
        // Bawaan = perilaku sebelum opsi ini ada: getar. Mengubahnya berarti mengubah rasa
        // ketukan seluruh pengguna lama saat pembaruan — keputusan itu harus sadar, bukan lupa.
        assertEquals("getar", UmpanBalikTasbih.BAWAAN)
    }

    @Test
    fun nilaiTidakDikenalDitarikKeBawaan() {
        assertEquals("getar", UmpanBalikTasbih.benarkan("bunyi"))
        assertEquals("getar", UmpanBalikTasbih.benarkan(""))
        assertEquals("diam", UmpanBalikTasbih.benarkan("diam"))
        assertEquals("beep", UmpanBalikTasbih.benarkan("beep"))
    }

    @Test
    fun volumeTerjepitDiDalamRentangnya() {
        assertEquals(0f, UmpanBalikTasbih.volume(-0.5f))
        assertEquals(1f, UmpanBalikTasbih.volume(1.5f))
        assertEquals(0.8f, UmpanBalikTasbih.volume(0.8f))
        // Bawaan harus berada di dalam rentang yang sah — kesalahan ketik di konstanta bawaan
        // tidak boleh lolos ke penyimpanan.
        assertTrue(UmpanBalikTasbih.VOL_BAWAAN in UmpanBalikTasbih.VOL_MIN..UmpanBalikTasbih.VOL_MAKS)
    }

    @Test
    fun persenMembulatkanDenganJepitYangSama() {
        assertEquals(80, UmpanBalikTasbih.persen(0.8f))
        assertEquals(0, UmpanBalikTasbih.persen(0f))
        assertEquals(100, UmpanBalikTasbih.persen(1f))
        // Nilai di luar rentang tidak boleh menghasilkan persen di luar jangkauan pemain beep.
        assertEquals(0, UmpanBalikTasbih.persen(-1f))
        assertEquals(100, UmpanBalikTasbih.persen(9f))
    }

    @Test
    fun labelChipSatuEjaan() {
        assertEquals("Diam", UmpanBalikTasbih.label("diam"))
        assertEquals("Getar", UmpanBalikTasbih.label("getar"))
        assertEquals("Beep", UmpanBalikTasbih.label("beep"))
        // Mode yang tak dikenal tetap berlabel — chip tidak pernah kosong di layar Pengaturan.
        assertEquals("Getar", UmpanBalikTasbih.label("aneh"))
    }
}
