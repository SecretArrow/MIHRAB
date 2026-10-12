package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Arah Kiblat" meminta *"Pengaturan satuan dan tampilan kompas"* dan meminta
 * perangkat tanpa magnetometer dilayani *"melalui tampilan arah berbasis peta atau sudut hasil
 * perhitungan, dengan penjelasan bahwa kompas langsung tidak tersedia"*.
 *
 * Yang mudah salah di sini bukan pilihannya, melainkan **jarumnya**: tanpa magnetometer, sudut
 * hadap perangkat tidak diketahui, sehingga jarum yang tetap digambar menunjuk arah yang salah
 * padahal tampak meyakinkan. Uji ini menjaga agar jarum hanya muncul saat sensor ada **dan**
 * pengguna memilih tampilan kompas, dan agar pilihannya benar-benar tersimpan serta terbaca.
 */
class TampilanKompasTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun kiblatScreen(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatScreen.kt")

    private fun store(): String = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")

    @Test
    fun pilihanYangDitawarkanSelarasDenganYangSah() {
        assertEquals(listOf(TampilanKompas.KOMPAS, TampilanKompas.ANGKA), TampilanKompas.SEMUA)
        TampilanKompas.SEMUA.forEach { assertTrue("'$it' seharusnya sah", TampilanKompas.sah(it)) }
    }

    @Test
    fun nilaiTakDikenalTidakSah() {
        listOf("", "jarum", "KOMPAS", "keduanya", "peta").forEach {
            assertFalse("'$it' seharusnya tidak sah", TampilanKompas.sah(it))
        }
    }

    @Test
    fun setiapPilihanPunyaLabelSendiri() {
        val label = TampilanKompas.SEMUA.map { TampilanKompas.label(it) }
        assertEquals(label.size, label.distinct().size)
        label.forEach { assertTrue("label kosong", it.isNotBlank()) }
    }

    /** Inti aturannya: jarum butuh sensor, dan pilihan yang tak dikenal jatuh ke tampilan kompas. */
    @Test
    fun jarumHanyaDigambarBilaSensorAdaDanTampilanMemilihKompas() {
        assertTrue(TampilanKompas.gambarJarum(TampilanKompas.KOMPAS, adaSensor = true))
        assertFalse("jarum digambar padahal tanpa magnetometer",
            TampilanKompas.gambarJarum(TampilanKompas.KOMPAS, adaSensor = false))
        assertFalse(TampilanKompas.gambarJarum(TampilanKompas.ANGKA, adaSensor = true))
        assertFalse(TampilanKompas.gambarJarum(TampilanKompas.ANGKA, adaSensor = false))
        assertTrue("pilihan tak dikenal harus jatuh ke tampilan kompas",
            TampilanKompas.gambarJarum("ngawur", adaSensor = true))
        assertFalse("pilihan tak dikenal tetap tidak boleh melawan sensor",
            TampilanKompas.gambarJarum("ngawur", adaSensor = false))
    }

    @Test
    fun kunciPreferensiDideklarasikanDanIkutBackup() {
        val s = store()
        assertTrue("Store.COMPASS_VIEW tidak dideklarasikan",
            s.contains("val COMPASS_VIEW = stringPreferencesKey(\"compass_view\")"))
        // Tanpa ikut importJson, pilihan tampilan kompas hilang saat backup dipulihkan.
        val impor = s.substringAfter("fun importJson")
        assertTrue("COMPASS_VIEW tidak ikut dipulihkan dari backup", impor.contains("COMPASS_VIEW"))
    }

    /** Jarum tidak boleh digambar di jalur mana pun yang melewatkan pemeriksaan sensor. */
    @Test
    fun layarKiblatMenggambarJarumHanyaLewatPenjaga() {
        val layar = kiblatScreen()
        assertTrue("KiblatScreen tidak memakai aturan TampilanKompas",
            layar.contains("TampilanKompas.gambarJarum(s.tampilan, s.hasCompass)"))
        val sesudah = layar.substringAfter("if (gambarJarum)")
        assertTrue("Canvas jarum tidak berada di dalam penjaga gambarJarum", sesudah.contains("Canvas("))
        assertEquals("ada Canvas kompas di luar penjaga gambarJarum",
            1, Regex("Canvas\\(").findAll(layar).count())
    }

    /** Kompas digambar, bukan teks — pembaca layar hanya tahu arahnya bila diberi label. */
    @Test
    fun jarumKompasPunyaLabelUntukPembacaLayar() {
        val blok = kiblatScreen().substringAfter("Canvas(").substringBefore("drawCircle(")
        assertTrue("jarum kompas tanpa contentDescription (TalkBack)", blok.contains("contentDescription"))
    }

    @Test
    fun pemilihTampilanDitawarkanDiLayarKiblatDanPengaturan() {
        val layar = kiblatScreen()
        assertTrue("layar Kiblat tidak menawarkan pemilih tampilan",
            layar.contains("TampilanKompas.SEMUA.forEach") && layar.contains("vm.setTampilan("))
        val pengaturan = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak menyediakan tampilan kompas",
            pengaturan.contains("TampilanKompas.SEMUA.forEach") && pengaturan.contains("Store.COMPASS_VIEW"))
    }

    /** ViewModel harus membaca dan menyimpan pilihannya, bukan hanya menyediakannya di layar. */
    @Test
    fun viewModelMembacaDanMenyimpanTampilan() {
        val vm = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatViewModel.kt")
        assertTrue("ViewModel tidak membaca preferensi tampilan",
            vm.contains("store.get(Store.COMPASS_VIEW"))
        assertTrue("ViewModel tidak menyimpan preferensi tampilan",
            vm.contains("store.set(Store.COMPASS_VIEW"))
    }
}
