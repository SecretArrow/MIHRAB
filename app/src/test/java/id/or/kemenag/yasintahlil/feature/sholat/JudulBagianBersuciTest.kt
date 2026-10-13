package id.or.kemenag.yasintahlil.feature.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.PanduanBersuci
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Judul kedua bagian layar Wudhu — *"Tata cara wudhu"* dan *"Tata cara tayamum"* — dibaca dari
 * `wudhu.json` (`wudhu.judul` / `tayamum.judul`), bukan diketik di layar.
 *
 * Sebelum ini keduanya hidup dua kali: sekali di berkasnya, sekali sebagai literal di `WudhuScreen`.
 * Salinan di layar bukan sekadar lebih pendek — judul tayamum di berkas berbunyi *"Tata cara tayamum
 * (pengganti wudhu saat tidak ada air / sakit)"*, dan keterangan di dalam tanda kurung itulah yang
 * menjawab kapan bagian itu dipakai. Yang tampil ke pengguna hanya *"Tata cara tayamum"*, sehingga
 * fakta yang sumbernya sendiri tulis tak pernah sampai ke layar, dan mengubah judul di berkasnya
 * tak mengubah layar sama sekali.
 *
 * Bentuk kekeliruannya sama dengan provenans `edisi` yang sudah diperbaiki lebih dahulu: konten
 * yang **ada** di bundel tetapi tak dibaca. Karena itu penjaganya juga sama bentuknya — memaku
 * bahwa berkasnya mencantumkan judulnya, bahwa repo membacanya, dan bahwa layar memakainya.
 */
class JudulBagianBersuciTest {

    private fun berkas(): JSONObject =
        JSONObject(KodeSumber.sumber("src/main/assets/content/wudhu.json"))

    private val layar get() = KodeSumber.kodeTanpaKomentar(
        KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/WudhuScreen.kt"))

    private val repo get() = KodeSumber.kodeTanpaKomentar(
        KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"))

    // ---- Berkasnya mencantumkan judulnya ---------------------------------------------------------

    @Test
    fun bundelMencantumkanJudulKeduaBagian() {
        listOf("wudhu", "tayamum").forEach { bagian ->
            val judul = berkas().getJSONObject(bagian).getString("judul")
            assertTrue("$bagian.judul kosong di wudhu.json", judul.isNotBlank())
        }
    }

    /**
     * Judul tayamum di berkas menyebut kapan bagian itu dipakai — dan itulah yang hilang saat layar
     * mengetik judulnya sendiri. Bila keterangan ini kelak dihapus dari berkasnya, penjaganya jangan
     * diam-diam tetap hijau: uji ini menuntut keterangannya benar-benar ada di sana.
     */
    @Test
    fun judulTayamumBundelMenyebutKapanDipakaikan() {
        val judul = berkas().getJSONObject("tayamum").getString("judul").lowercase()
        assertTrue("judul tayamum di bundel tidak lagi menyebut fungsinya sebagai pengganti wudhu: $judul",
            judul.contains("pengganti wudhu"))
    }

    /** Dua bagian yang berbeda tak boleh berjudul sama — layarnya akan tampak seperti satu bagian. */
    @Test
    fun judulKeduaBagianBerbeda() {
        assertNotEquals("judul wudhu dan tayamum sama di wudhu.json",
            berkas().getJSONObject("wudhu").getString("judul"),
            berkas().getJSONObject("tayamum").getString("judul"))
    }

    // ---- Repo membacanya ------------------------------------------------------------------------

    @Test
    fun repoMembacaJudulBagianDariObjeknya() {
        assertTrue("ContentRepo tidak membaca judul bagian wudhu dari objeknya",
            repo.contains("judulWudhu = judulBagian(\"wudhu\")"))
        assertTrue("ContentRepo tidak membaca judul bagian tayamum dari objeknya",
            repo.contains("judulTayamum = judulBagian(\"tayamum\")"))
        assertTrue("ContentRepo tidak membaca kunci 'judul' dari objek bagiannya",
            repo.contains("optJSONObject(key)?.optString(\"judul\")"))
    }

    /** Tanpa pembacaan ini, medan baru hanya akan bernilai bawaan kosong di seluruh aplikasi. */
    @Test
    fun bawaanModelKosongBukanTeksTertanam() {
        assertEquals("bawaan judulWudhu bukan string kosong",
            "", PanduanBersuci().judulWudhu)
        assertEquals("bawaan judulTayamum bukan string kosong",
            "", PanduanBersuci().judulTayamum)
    }

    // ---- Layar memakainya -----------------------------------------------------------------------

    @Test
    fun layarMemakaiJudulDariBundel() {
        assertTrue("layar tidak memakai judul bagian wudhu dari bundel",
            layar.contains("LangkahBagian(panduan.judulWudhu"))
        assertTrue("layar tidak memakai judul bagian tayamum dari bundel",
            layar.contains("LangkahBagian(panduan.judulTayamum"))
    }

    /**
     * Yang dijaga bukan sekadar bahwa medannya dipakai, melainkan bahwa **tidak ada** judul bagian
     * yang diketik langsung di layar: satu heading yang lolos sebagai literal sudah cukup untuk
     * memutus kaitannya dengan berkasnya. Karena itu argumen judul `LangkahBagian` tak boleh berupa
     * literal sama sekali — harus lewat medan bundelnya.
     */
    @Test
    fun layarTidakMengetikJudulBagianSendiri() {
        assertTrue("layar mengetik judul bagian sebagai literal, bukan membacanya dari bundel",
            !layar.contains("LangkahBagian(\""))
    }
}
