package id.or.kemenag.yasintahlil.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Berkas Qur'an di `assets/content` mencatat **tiga** asal teks secara terpisah: `sumber_arab`
 * (Mushaf Standar Indonesia LPMQ Kemenag), `sumber_latin` (transliterasi resmi Kemenag), dan
 * `sumber_terjemahan` (terjemah edisi 2019). Model `Surah` hanya membawa yang terakhir, dan layar
 * Yasin tidak menampilkannya sama sekali — judulnya hanya menulis "(MSI Kemenag)" sebagai hardcode.
 *
 * Uji ini menjaga agar ketiganya dibaca, disusun menjadi satu baris yang jujur, dan ditampilkan.
 */
class AtribusiTeksQuranTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun surah(arab: String = "", latin: String = "", terjemahan: String = "") =
        Surah("Yasin", 1, emptyList(), terjemahan, "quran", arab, latin)

    // ---- Aturan (murni, diuji di JVM) ----

    @Test
    fun ketigaSumberTampilDenganLabelnya() {
        val s = surah("MSI LPMQ Kemenag", "Transliterasi Kemenag", "Terjemah Kemenag 2019")
        assertEquals(
            "Teks Arab: MSI LPMQ Kemenag · Transliterasi: Transliterasi Kemenag · " +
                "Terjemahan: Terjemah Kemenag 2019",
            AtribusiTeks.surah(s),
        )
    }

    /** Yang kosong tidak boleh muncul sebagai "Teks Arab: " tanpa isi. */
    @Test
    fun sumberKosongDilewatiBukanDitampilkanKosong() {
        assertEquals("Terjemahan: Terjemah Kemenag 2019",
            AtribusiTeks.surah(surah(terjemahan = "Terjemah Kemenag 2019")))
        assertEquals("Teks Arab: MSI LPMQ Kemenag",
            AtribusiTeks.surah(surah(arab = "MSI LPMQ Kemenag")))
        assertEquals("", AtribusiTeks.surah(surah()))
    }

    /** Spasi saja dianggap kosong — bukan sumber yang diklaim. */
    @Test
    fun sumberBerisiSpasiDianggapTidakAda() {
        assertEquals("", AtribusiTeks.surah(surah(arab = "   ", latin = "\t")))
    }

    // ---- Pembacaan bundel ----

    @Test
    fun repoMembacaSumberArabDanLatin() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt")
        val badan = s.substringAfter("fun surah(file: String)").substringBefore("val yasin")
        assertTrue("ContentRepo tidak membaca sumber_arab", badan.contains("optString(\"sumber_arab\""))
        assertTrue("ContentRepo tidak membaca sumber_latin", badan.contains("optString(\"sumber_latin\""))
    }

    /** Cadangan itu hanya berguna bila berkas Qur'annya memang mencantumkan ketiganya. */
    @Test
    fun berkasQuranMencantumkanKetigaSumber() {
        listOf("yasin_full.json", "surah_001.json", "surah_112.json", "ayat_kursi.json").forEach { nama ->
            val json = sumber("src/main/assets/content/$nama")
            assertTrue("$nama tidak mencantumkan sumber_arab", json.contains("\"sumber_arab\""))
            assertTrue("$nama tidak mencantumkan sumber_latin", json.contains("\"sumber_latin\""))
            assertTrue("$nama tidak mencantumkan sumber_terjemahan", json.contains("\"sumber_terjemahan\""))
        }
    }

    // ---- Layar ----

    @Test
    fun layarYasinMenampilkanAtribusiTeks() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/yasin/YasinScreen.kt")
        assertTrue("layar Yasin tidak memakai AtribusiTeks", layar.contains("AtribusiTeks.surah("))
        assertFalse("layar Yasin masih mengarang sumber di judulnya",
            layar.contains("ayat (MSI Kemenag)"))
    }
}
