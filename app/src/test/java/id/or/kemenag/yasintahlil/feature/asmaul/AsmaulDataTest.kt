package id.or.kemenag.yasintahlil.feature.asmaul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menjaga keutuhan daftar [AsmaulData] — yang paling sunyi ketika rusak adalah jumlahnya: satu
 * nama yang terpotong dari daftar tidak memunculkan galat apa pun di layar, pengguna hanya
 * melihat daftar yang "sepertinya lengkap". Uji ini memastikan 99 nama hadir utuh, bernomor
 * berurutan, berharakat, tanpa ganda.
 */
class AsmaulDataTest {

    @Test
    fun `jumlahnya tepat sembilan puluh sembilan`() {
        assertEquals(99, AsmaulData.DAFTAR.size)
    }

    @Test
    fun `nomornya berurutan satu sampai sembilan puluh sembilan tanpa ganda`() {
        assertEquals((1..99).toList(), AsmaulData.DAFTAR.map { it.no })
    }

    @Test
    fun `tidak ada kolom yang kosong`() {
        AsmaulData.DAFTAR.forEach { n ->
            assertTrue("arab kosong pada nama ke-${n.no}", n.arab.isNotBlank())
            assertTrue("latin kosong pada nama ke-${n.no}", n.latin.isNotBlank())
            assertTrue("arti kosong pada nama ke-${n.no}", n.arti.isNotBlank())
        }
    }

    /**
     * Tulisan Arab setiap nama wajib berharakat — pengguna yang baru belajar membaca tidak dapat
     * membinakan `ك ب ر` yang telanjang, dan itulah alasan daftar ini ditulis berharakat penuh.
     */
    @Test
    fun `setiap tulisan arab berharakat`() {
        AsmaulData.DAFTAR.forEach { n ->
            val berharakat = n.arab.any { it in '\u064B'..'\u0652' }
            assertTrue("nama ke-${n.no} (${n.latin}) tidak berharakat", berharakat)
        }
    }

    /**
     * Latin yang ganda membuat dua kartu berbedang Arab tampak sebagai satu nama yang tercantum
     * dua kali. Satu pasang memang sah dan didokumentasikan: Al-Majid ditulis `الْمَجِيدُ` (ke-48)
     * dan `الْمَاجِدُ` (ke-65) — dua lafaz berbeda yang kebetulan beralihaksara sama, bukan
     * salinan; karenanya yang dijaga 98 latin berbeda, bukan 99.
     */
    @Test
    fun `latinnya nyaris seluruhnya unik`() {
        val unik = AsmaulData.DAFTAR.map { it.latin }.toSet()
        assertEquals(98, unik.size)
    }

    @Test
    fun `daftar dibuka Allah dan ditutup Ash-Shabur`() {
        assertEquals("Allah", AsmaulData.DAFTAR.first().latin)
        assertEquals("Ash-Shabur", AsmaulData.DAFTAR.last().latin)
        assertEquals("Ar-Rahman", AsmaulData.DAFTAR[1].latin)
    }
}
