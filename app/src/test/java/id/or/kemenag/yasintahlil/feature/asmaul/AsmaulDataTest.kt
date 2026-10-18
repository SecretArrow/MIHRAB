package id.or.kemenag.yasintahlil.feature.asmaul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga keutuhan daftar Asmaul Husna di `assets/asmaul/asmaul_husna.json` — yang paling sunyi
 * ketika rusak adalah jumlahnya: satu nama yang terpotong tidak memunculkan galat apa pun di
 * layar, pengguna hanya melihat daftar yang "sepertinya lengkap". Uji ini memastikan 99 nama
 * hadir utuh, bernomor berurutan, berharakat, tanpa ganda (kecuali satu pasang yang sah dan
 * dijelaskan).
 *
 * Daftarnya dibaca sebagai teks berkas — bukan lewat [AsmaulData] — supaya yang diuji adalah
 * berkas yang benar-benar dibundel, bukan salinan yang kebetulan masih di memori (pola uji
 * parser berkas ala `KodeSumber`).
 */
class AsmaulDataTest {

    private fun berkas(): File =
        listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
            .let { File(it, "src/main/assets/asmaul/asmaul_husna.json") }

    /** Pasangan kunci–nilai satu entri, bentuk apa adanya di berkas. */
    private fun entri(): List<String> =
        Regex("\"arab\"\\s*:\\s*\"[^\"]*\"\\s*,\\s*\"latin\"\\s*:\\s*\"[^\"]*\"[^}]*")
            .findAll(berkas().readText()).map { it.value }.toList()

    private fun nilai(entri: String, kunci: String): String =
        Regex("\"$kunci\"\\s*:\\s*\"([^\"]*)\"").find(entri)!!.groupValues[1]

    @Test
    fun `jumlahnya tepat sembilan puluh sembilan`() {
        assertEquals(99, entri().size)
    }

    @Test
    fun `nomornya berurutan satu sampai sembilan puluh sembilan tanpa ganda`() {
        val nomor = Regex("\"no\"\\s*:\\s*(\\d+)")
            .findAll(berkas().readText()).map { it.groupValues[1].toInt() }.toList()
        assertEquals((1..99).toList(), nomor)
    }

    @Test
    fun `tidak ada kolom yang kosong`() {
        entri().forEachIndexed { i, e ->
            assertTrue("arab kosong pada entri ke-${i + 1}", nilai(e, "arab").isNotBlank())
            assertTrue("latin kosong pada entri ke-${i + 1}", nilai(e, "latin").isNotBlank())
            assertTrue("arti kosong pada entri ke-${i + 1}", nilai(e, "arti").isNotBlank())
        }
    }

    /**
     * Tulisan Arab setiap nama wajib berharakat — pengguna yang baru belajar membaca tidak dapat
     * membinakan huruf yang telanjang, dan itulah alasan daftar ini ditulis berharakat penuh.
     */
    @Test
    fun `setiap tulisan arab berharakat`() {
        entri().forEachIndexed { i, e ->
            val arab = nilai(e, "arab")
            val berharakat = arab.any { it in '\u064B'..'\u0652' }
            assertTrue("entri ke-${i + 1} (${nilai(e, "latin")}) tidak berharakat", berharakat)
        }
    }

    /**
     * Latin yang ganda membuat dua entri berbedang Arab tampak sebagai satu nama yang tercantum
     * dua kali. Satu pasang memang sah: Al-Majid ditulis `الْمَجِيدُ` dan `الْمَاجِدُ` — dua
     * lafaz berbeda yang kebetulan beralihaksara sama, bukan salinan; karenanya yang dijaga 98
     * latin berbeda, bukan 99.
     */
    @Test
    fun `latinnya nyaris seluruhnya unik`() {
        val unik = entri().map { nilai(it, "latin") }.toSet()
        assertEquals(98, unik.size)
    }

    @Test
    fun `daftar dibuka Allah dan ditutup Ash-Shabur`() {
        val semua = entri()
        assertEquals("Allah", nilai(semua.first(), "latin"))
        assertEquals("Ash-Shabur", nilai(semua.last(), "latin"))
        assertEquals("Ar-Rahman", nilai(semua[1], "latin"))
    }

    /**
     * Penjelasan asal-usul di kunci `keterangan` adalah satu-satunya tempat daftar ini
     * mendokumentasikan batas pengetahuannya (daftar masyhur, variasi antar sumber) — hilangnya
     * berarti layar menampilkan 99 nama tanpa pernah menjelaskan dari mana asalnya.
     */
    @Test
    fun `keterangan asal usul hadir`() {
        val keterangan = Regex("\"keterangan\"\\s*:\\s*\"([^\"]*)\"")
            .find(berkas().readText())!!.groupValues[1]
        assertTrue(keterangan.length > 100)
    }
}
