package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` meminta pengaturan *"tema halaman"* pada modul bacaan, di samping ukuran huruf dan
 * jarak antarbaris.
 *
 * Tema halaman mudah berubah menjadi kontrol mati: pilihannya tersimpan, tetapi tak ada layar yang
 * membacanya. Uji ini menjaga rantai lengkapnya — pilihan yang sah dan berlabel, kunci preferensi
 * yang dideklarasikan **dan** ikut backup, akar aplikasi yang menyediakannya, kartu bacaan yang
 * menghormatinya, serta pemilihnya yang benar-benar ditawarkan di bottom sheet pengaturan bacaan.
 */
class TemaHalamanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun store(): String = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")

    private fun uiKit(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt")

    private fun theme(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/core/designsystem/Theme.kt")

    private fun mainActivity(): String = sumber("src/main/java/id/or/kemenag/yasintahlil/MainActivity.kt")

    @Test
    fun pilihanYangDitawarkanSelarasDenganYangSah() {
        assertEquals(listOf(TemaHalaman.IKUT, TemaHalaman.KREM, TemaHalaman.GELAP), TemaHalaman.SEMUA)
        TemaHalaman.SEMUA.forEach { assertTrue("'$it' seharusnya sah", TemaHalaman.sah(it)) }
    }

    @Test
    fun nilaiTakDikenalTidakSah() {
        listOf("", "biru", "IKUT", "terang").forEach {
            assertFalse("'$it' seharusnya tidak sah", TemaHalaman.sah(it))
        }
    }

    /** Tiap pilihan harus punya namanya sendiri, jika tidak pengguna tak bisa membedakannya. */
    @Test
    fun setiapPilihanPunyaLabelSendiri() {
        val label = TemaHalaman.SEMUA.map { TemaHalaman.label(it) }
        assertEquals(label.size, label.distinct().size)
        label.forEach { assertTrue("label kosong", it.isNotBlank()) }
    }

    @Test
    fun kunciPreferensiDideklarasikanDanIkutBackup() {
        val s = store()
        assertTrue("Store.PAGE_THEME tidak dideklarasikan",
            s.contains("val PAGE_THEME = stringPreferencesKey(\"page_theme\")"))
        // Tanpa ikut importJson, pilihan tema halaman hilang saat backup dipulihkan.
        val impor = s.substringAfter("fun importJson")
        assertTrue("PAGE_THEME tidak ikut dipulihkan dari backup", impor.contains("PAGE_THEME"))
    }

    @Test
    fun akarAplikasiMenyediakanTemaHalamanDariPreferensi() {
        val m = mainActivity()
        assertTrue("MainActivity tidak menyediakan tema halaman ke seluruh layar",
            m.contains("ProvideTemaHalaman("))
        assertTrue("MainActivity tidak membaca preferensi tema halaman",
            m.contains("pageTheme"))
    }

    @Test
    fun kartuBacaanMenghormatiTemaHalaman() {
        val ui = uiKit()
        val badan = ui.substringAfter("fun BacaanCard(").substringBefore("\nfun ")
        assertTrue("BacaanCard tidak membaca LocalTemaHalaman", badan.contains("LocalTemaHalaman.current"))
        assertTrue("BacaanCard tidak memetakan tema ke skema warna", badan.contains("skemaHalaman("))
    }

    @Test
    fun setiapPilihanPunyaSkemaWarnaSendiri() {
        val skema = theme().substringAfter("fun skemaHalaman(")
        assertTrue("skema krem hilang", skema.contains("TemaHalaman.KREM"))
        assertTrue("skema gelap hilang", skema.contains("TemaHalaman.GELAP"))
        assertTrue("tema halaman memakai warna kertas yang sama untuk krem dan gelap",
            skema.contains("Palette.KERTAS_KREM") && skema.contains("Palette.KERTAS_GELAP"))
    }

    @Test
    fun pemilihTemaDitawarkanDiPengaturanBacaan() {
        val ui = uiKit()
        val sheet = ui.substringAfter("fun BacaanSettingsSheet(").substringBefore("\nprivate fun TemaHalamanChips")
        assertTrue("bottom sheet pengaturan bacaan tidak menawarkan pemilih tema halaman",
            sheet.contains("TemaHalamanChips()"))
        val pemilih = ui.substringAfter("private fun TemaHalamanChips()")
        assertTrue("pemilih tidak menawarkan seluruh pilihan", pemilih.contains("TemaHalaman.SEMUA.forEach"))
        assertTrue("pemilih tidak menyimpan pilihannya", pemilih.contains("Store.PAGE_THEME"))
    }
}
