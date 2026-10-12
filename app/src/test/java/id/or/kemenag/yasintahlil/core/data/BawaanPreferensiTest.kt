package id.or.kemenag.yasintahlil.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Bawaan preferensi hanya boleh ada di satu tempat ([BawaanPreferensi]).
 *
 * Uji ini menjaga dua hal. Pertama, nilainya sendiri: apa yang dianggap "belum diatur" pengguna.
 * Kedua — dan ini yang lebih penting — bahwa tidak ada tempat lain yang menuliskannya kembali
 * sebagai literal. Sebelumnya `store.get(Store.SHOW_ARAB, true)` muncul di sebelah layar,
 * `store.get(Store.NOTIF, true)` di empat berkas berbeda, dan batas koreksi Hijriah `-2`/`2` di dua
 * tombol; begitu satu tempat mengubah bawaannya, yang lain diam-diam tidak ikut berubah.
 *
 * Pemindaian sumber ini membaca **kode tanpa komentar**: KDoc [BawaanPreferensi] sengaja menyebut
 * pola lama (`store.get(Store.SHOW_ARAB, true)`) sebagai penjelasan, dan penjelasan itu bukan
 * pelanggaran.
 */
class BawaanPreferensiTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumberKotlin(): List<File> =
        File(root(), "src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Sumber tanpa komentar dan tanpa isi literal string. */
    private fun tanpaKomentar(teks: String): String {
        val sb = StringBuilder()
        var i = 0
        var blok = 0
        while (i < teks.length) {
            when {
                blok > 0 ->
                    if (teks.startsWith("*/", i)) { blok--; i += 2 } else i++
                teks.startsWith("//", i) -> {
                    val akhir = teks.indexOf('\n', i)
                    i = if (akhir < 0) teks.length else akhir
                }
                teks.startsWith("/*", i) -> { blok++; i += 2 }
                teks.startsWith("\"\"\"", i) -> {
                    val akhir = teks.indexOf("\"\"\"", i + 3)
                    i = if (akhir < 0) teks.length else akhir + 3
                }
                teks[i] == '"' -> {
                    i++
                    while (i < teks.length && teks[i] != '"' && teks[i] != '\n') {
                        i += if (teks[i] == '\\') 2 else 1
                    }
                    i++
                }
                else -> { sb.append(teks[i]); i++ }
            }
        }
        return sb.toString()
    }

    @Test
    fun bawaanTampilanMenampilkanSeluruhBacaan() {
        assertTrue("Arab tidak tampil secara bawaan", BawaanPreferensi.TAMPIL_ARAB)
        assertTrue("Latin tidak tampil secara bawaan", BawaanPreferensi.TAMPIL_LATIN)
        assertTrue("terjemahan tidak tampil secara bawaan", BawaanPreferensi.TAMPIL_TERJEMAHAN)
    }

    @Test
    fun bawaanPengingatSesuaiSifatnyaMasingMasing() {
        assertTrue("pengingat waktu sholat mati secara bawaan", BawaanPreferensi.PENGINGAT_WAKTU)
        assertFalse("pengingat doa adzan menyala sendiri tanpa diminta",
            BawaanPreferensi.PENGINGAT_DOA_ADZAN)
        assertFalse("pengingat iqomah menyala sendiri tanpa diminta",
            BawaanPreferensi.PENGINGAT_IQOMAH)
        assertTrue("kartu info beranda tidak tampil secara bawaan", BawaanPreferensi.KARTU_INFO)
    }

    @Test
    fun koreksiHijriahTerjagaDiDalamRentangnya() {
        assertTrue("bawaan koreksi Hijriah di luar rentangnya",
            BawaanPreferensi.KOREKSI_HIJRIAH in BawaanPreferensi.RENTANG_KOREKSI_HIJRIAH)
        assertEquals(-2, BawaanPreferensi.koreksiHijriah(-9))
        assertEquals(2, BawaanPreferensi.koreksiHijriah(9))
        assertEquals(0, BawaanPreferensi.koreksiHijriah(0))
        assertEquals(-1, BawaanPreferensi.koreksiHijriah(-1))
    }

    /** Bawaan "belum ada apa-apa" — dibedakan dari "ada, tapi kosong" oleh layar pemakainya. */
    @Test
    fun bawaanBelumAdaIsi() {
        assertEquals("", BawaanPreferensi.TERAKHIR_DIBACA)
        assertEquals("", BawaanPreferensi.BERKAS_ADZAN)
        assertEquals("", BawaanPreferensi.DAFTAR_SUARA_ADZAN)
        assertEquals("", BawaanPreferensi.SUSUNAN_SAYA)
        assertEquals("", BawaanPreferensi.SUSUNAN_TAHLIL_DIPILIH)
        assertEquals("", BawaanPreferensi.RIWAYAT)
        assertEquals(0, BawaanPreferensi.PENANDA_YASIN)
        assertTrue(BawaanPreferensi.FAVORIT.isEmpty())
        assertFalse("jadwal ikut lokasi tanpa diminta", BawaanPreferensi.JADWAL_IKUT_LOKASI)
        assertFalse("koordinat GPS dipakai tanpa diminta", BawaanPreferensi.LOKASI_DARI_GPS)
    }

    /**
     * Invarian yang menjaga seluruh kelas kerusakan ini: pembacaan preferensi tidak boleh membawa
     * bawaannya sendiri sebagai literal. Hanya `Store` dan objek aturan bersama yang boleh
     * menyebutkannya.
     */
    @Test
    fun bawaanTidakDitulisUlangDiSumber() {
        val pola = Regex("""\b(get|flow|alir)\(\s*Store\.[A-Z_]+\s*,\s*(true|false|0|0f|""|emptySet\(\))""")
        val pelanggar = mutableListOf<String>()
        sumberKotlin().forEach { f ->
            tanpaKomentar(f.readText()).lines().forEachIndexed { no, baris ->
                if (pola.containsMatchIn(baris)) pelanggar += "${f.name}:${no + 1}: ${baris.trim()}"
            }
        }
        assertTrue(
            "bawaan preferensi ditulis ulang sebagai literal, bukan dari BawaanPreferensi: $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /** `Store` sendiri pun memakai nilai bersama itu, bukan menuliskannya sendiri. */
    @Test
    fun storeMemakaiBawaanBersama() {
        val s = File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt").readText()
        assertTrue("Store.toggleFav menulis bawaan favoritnya sendiri",
            s.contains("BawaanPreferensi.FAVORIT"))
        assertTrue("Store.pushHist menulis bawaan riwayatnya sendiri",
            s.contains("BawaanPreferensi.RIWAYAT"))
    }
}
