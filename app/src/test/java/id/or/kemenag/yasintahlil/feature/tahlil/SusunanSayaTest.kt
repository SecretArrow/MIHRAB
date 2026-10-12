package id.or.kemenag.yasintahlil.feature.tahlil

import id.or.kemenag.yasintahlil.core.data.TahlilItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji aturan "Susunan saya" (`plan.txt`: *"Sediakan susunan bacaan yang dapat dikonfigurasi"*).
 * [SusunanSaya] sengaja murni (tanpa Android) supaya aturan urutan dan penyaringan bagian dapat
 * diuji di JVM. Yang diuji di sini hanya **pilihan dan urutan** bagian — teks bacaan tidak pernah
 * disentuh, sehingga tidak ada teks keagamaan yang dikarang atau disusun ulang.
 */
class SusunanSayaTest {

    private fun item(id: String, judul: String = "Bagian $id") = TahlilItem(id = id, judul = judul)

    @Test
    fun parseMembuangDuplikatDanBagianKosong() {
        assertEquals(listOf("a", "b", "c"), SusunanSaya.parse("a, b ,,a,c"))
        assertTrue(SusunanSaya.parse("").isEmpty())
        assertTrue(SusunanSaya.parse(" , , ").isEmpty())
    }

    @Test
    fun serializeDanParseSalingMembalikkan() {
        val ids = listOf("a", "b", "c")
        assertEquals(ids, SusunanSaya.parse(SusunanSaya.serialize(ids)))
        // Duplikat dibuang saat serialisasi, jadi nilai tersimpan selalu bersih.
        assertEquals("a,b", SusunanSaya.serialize(listOf("a", " b ", "a")))
    }

    @Test
    fun susunMengikutiUrutanPilihanBukanUrutanBerkas() {
        val semua = listOf(item("a"), item("b"), item("c"))
        assertEquals(listOf("c", "a"), SusunanSaya.susun(semua, listOf("c", "a")).map { it.id })
    }

    @Test
    fun susunHanyaMemuatBagianYangDipilih() {
        val semua = listOf(item("a"), item("b"), item("c"))
        assertEquals(listOf("b"), SusunanSaya.susun(semua, listOf("b")).map { it.id })
    }

    /**
     * Id yang tidak dikenal — mis. bagian yang dihapus dari bundel konten setelah susunan
     * disimpan — harus dilewati, bukan membuat layar gagal.
     */
    @Test
    fun susunMelewatiIdYangTidakDikenal() {
        val semua = listOf(item("a"), item("b"))
        assertEquals(listOf("b", "a"), SusunanSaya.susun(semua, listOf("hilang", "b", "a")).map { it.id })
    }

    /**
     * Bila seluruh id tak dikenal, hasilnya kosong. Inilah alasan `TahlilViewModel` menolak
     * menyimpan susunan kosong: susunan tanpa bagian hanya akan membuat layar buntu.
     */
    @Test
    fun pilihanYangSeluruhnyaTidakDikenalMenghasilkanDaftarKosong() {
        assertTrue(SusunanSaya.susun(listOf(item("a")), listOf("x", "y")).isEmpty())
    }

    @Test
    fun pilihanKosongTidakMenghasilkanSusunan() {
        assertNull(SusunanSaya.susunan(emptyList()))
    }

    @Test
    fun pilihanTidakKosongMenghasilkanSusunanBuatanPengguna() {
        val s = SusunanSaya.susunan(listOf("a", "b"))
        assertEquals(SusunanSaya.ID, s?.id)
        assertEquals(SusunanSaya.NAMA, s?.nama)
        assertEquals(listOf("a", "b"), s?.bagian)
    }

    /**
     * Susunan buatan pengguna harus dibedakan dari susunan yang dibaca dari berkas konten,
     * supaya layar tidak salah menyebut asal susunan.
     */
    @Test
    fun idSusunanSayaBerbedaDariIdBerkasKonten() {
        assertEquals("saya", SusunanSaya.ID)
    }
}
