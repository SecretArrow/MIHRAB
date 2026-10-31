package id.or.kemenag.yasintahlil.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji aturan pencocokan pencarian terpadu. [Pencarian] sengaja murni (tanpa Android) supaya
 * perilaku pencarian — termasuk pencocokan nomor ayat yang persis dan pencarian teks Arab —
 * dapat diuji di sini, bukan hanya lewat pengamatan manual di perangkat.
 */
class PencarianTest {

    private fun doc(
        id: String,
        judul: String = "Judul $id",
        teks: List<String> = listOf("teks $id"),
        rute: String = "rute/$id",
        nomor: Int? = null,
    ) = SearchDoc(id = id, judul = judul, teks = teks, rute = rute, nomor = nomor)

    @Test
    fun kataKosongTidakMenghasilkanHasil() {
        val docs = listOf(doc("a"), doc("b"))
        assertTrue(Pencarian.cari(docs, "").isEmpty())
        assertTrue("Spasi saja juga berarti kata kosong", Pencarian.cari(docs, "   ").isEmpty())
    }

    @Test
    fun pencocokanTidakPekaHurufBesarKecil() {
        val docs = listOf(doc("a", teks = listOf("Doa Sebelum Makan")))
        assertEquals(1, Pencarian.cari(docs, "sebelum").size)
        assertEquals(1, Pencarian.cari(docs, "SEBELUM").size)
    }

    @Test
    fun nomorAyatDicocokkanPersis() {
        val docs = listOf(
            doc("ayat1", nomor = 1, teks = listOf("terjemahan pertama")),
            doc("ayat12", nomor = 12, teks = listOf("terjemahan kedua belas")),
        )
        val hasil = Pencarian.cari(docs, "1")
        assertEquals("Hanya ayat 1, bukan ayat 12", listOf("ayat1"), hasil.map { it.id })
    }

    @Test
    fun teksArabDapatDicari() {
        val docs = listOf(
            doc("arab", teks = listOf("اللهُ أَكْبَرُ")),
            doc("lain", teks = listOf("tidak memuat")),
        )
        assertEquals(listOf("arab"), Pencarian.cari(docs, "أَكْبَرُ").map { it.id })
    }

    @Test
    fun cuplikanDiambilDariBagianYangMemuatKataKunci() {
        val docs = listOf(
            doc("a", teks = listOf("awal yang tidak memuat apa-apa", "bagian ini memuat kata kunci khusus")),
        )
        assertEquals("bagian ini memuat kata kunci khusus", Pencarian.cari(docs, "kunci").single().cuplikan)
    }

    @Test
    fun cuplikanPanjangDipangkasDanDitandaiElipsis() {
        val panjang = "kata " + "a".repeat(300)
        val hasil = Pencarian.cari(listOf(doc("a", teks = listOf(panjang))), "kata").single()
        assertTrue("Cuplikan harus dipangkas", hasil.cuplikan.length <= 120)
        assertTrue("Cuplikan yang dipangkas ditandai elipsis", hasil.cuplikan.endsWith("…"))
    }

    @Test
    fun cuplikanMerapikanBarisBaruDanSpasiBerlebih() {
        val docs = listOf(doc("a", teks = listOf("baris satu\n\n   baris   dua")))
        assertEquals("baris satu baris dua", Pencarian.cari(docs, "dua").single().cuplikan)
    }

    @Test
    fun urutanHasilMengikutiUrutanIndeks() {
        val docs = listOf(
            doc("a", teks = listOf("sama")),
            doc("b", teks = listOf("sama")),
            doc("c", teks = listOf("sama")),
        )
        assertEquals(listOf("a", "b", "c"), Pencarian.cari(docs, "sama").map { it.id })
    }

    @Test
    fun batasJumlahHasilDihormati() {
        val docs = (1..20).map { doc("d$it", teks = listOf("kata yang sama")) }
        assertEquals(5, Pencarian.cari(docs, "kata", batas = 5).size)
        assertEquals("Batas nol tidak boleh mengosongkan hasil", 1, Pencarian.cari(docs, "kata", batas = 0).size)
    }

    @Test
    fun hasilMembawaJudulDanRuteDokumen() {
        val docs = listOf(doc("a", judul = "Surat Yasin", rute = "yasin", teks = listOf("yasin")))
        val hasil = Pencarian.cari(docs, "yasin").single()
        assertEquals("Surat Yasin", hasil.judul)
        assertEquals("yasin", hasil.rute)
    }
}
