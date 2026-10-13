package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
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
        assertEquals("", BawaanPreferensi.PROGRES_TAHLIL)
        assertEquals(0, BawaanPreferensi.PENANDA_YASIN)
        assertTrue(BawaanPreferensi.FAVORIT.isEmpty())
        assertFalse("jadwal ikut lokasi tanpa diminta", BawaanPreferensi.JADWAL_IKUT_LOKASI)
        assertFalse("koordinat GPS dipakai tanpa diminta", BawaanPreferensi.LOKASI_DARI_GPS)
    }

    /**
     * Invarian yang menjaga seluruh kelas kerusakan ini: pembacaan preferensi tidak boleh membawa
     * bawaannya sendiri sebagai literal. Hanya `Store` dan objek aturan bersama yang boleh
     * menyebutkannya.
     *
     * Pemindaiannya membuang komentar dan isi string lewat [KodeSumber]: KDoc kelas ini sengaja
     * menyebut pola lama (`store.get(Store.SHOW_ARAB, true)`) sebagai penjelasan, dan penjelasan itu
     * bukan pelanggaran.
     */
    @Test
    fun bawaanTidakDitulisUlangDiSumber() {
        val pola = Regex("""\b(get|flow|alir)\(\s*Store\.[A-Z_]+\s*,\s*(true|false|0|0f|""|emptySet\(\))""")
        val pelanggar = mutableListOf<String>()
        sumberKotlin().forEach { f ->
            KodeSumber.kodeTanpaKomentar(f.readText(), buangString = true)
                .lines().forEachIndexed { no, baris ->
                    if (pola.containsMatchIn(baris)) pelanggar += "${f.name}:${no + 1}: ${baris.trim()}"
                }
        }
        assertTrue(
            "bawaan preferensi ditulis ulang sebagai literal, bukan dari BawaanPreferensi: $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /**
     * Pemindah data warisan pun memakai nilai bersama itu, bukan menuliskannya sendiri.
     *
     * Sebelumnya yang diperiksa di sini adalah `Store.toggleFav`/`Store.pushHist` — penulis DataStore
     * dari versi lama. Keduanya sudah tidak ada (Room yang menulisnya kini), dan yang benar-benar
     * membaca ketiga kunci warisan itu adalah `MigrasiWarisan`; di situlah bawaannya harus datang
     * dari [BawaanPreferensi].
     */
    @Test
    fun migrasiWarisanMemakaiBawaanBersama() {
        val s = File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/data/local/MigrasiWarisan.kt").readText()
        assertTrue("pemindah favorit menulis bawaan favoritnya sendiri",
            s.contains("BawaanPreferensi.FAVORIT"))
        assertTrue("pemindah riwayat menulis bawaan riwayatnya sendiri",
            s.contains("BawaanPreferensi.RIWAYAT"))
        assertTrue("pemindah posisi Tahlil menulis bawaan posisi Tahlilnya sendiri",
            s.contains("BawaanPreferensi.PROGRES_TAHLIL"))
    }
}
