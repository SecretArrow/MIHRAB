package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Mode tema antarmuka (`plan.txt`: *"mode gelap/terang"*).
 *
 * Sebelumnya nilai yang sah dan artinya tersebar di tiga berkas: `MainViewModel` menulis bawaan
 * `"sistem"`, `MainActivity` memetakan `"terang"`/`"gelap"` ke `dark: Boolean`, dan layar Pengaturan
 * menyusun sendiri daftar chip `listOf("terang", "gelap", "sistem")`. Menambah satu mode berarti harus
 * ingat menyunting ketiganya; mode yang lolos dari pemetaan diam-diam jatuh ke "ikut sistem" sehingga
 * pilihan pengguna tampak tersimpan tetapi tidak berpengaruh. Uji ini menjaga agar daftar, bawaannya,
 * dan pemetaannya tetap satu, di [TemaAplikasi].
 */
class TemaAplikasiTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun aturan(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/core/designsystem/TemaAplikasi.kt")

    private fun mainActivity(): String = sumber("src/main/java/id/or/kemenag/yasintahlil/MainActivity.kt")

    private fun mainViewModel(): String = sumber("src/main/java/id/or/kemenag/yasintahlil/MainViewModel.kt")

    private fun pengaturan(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")

    @Test
    fun daftarModeSelarasDenganYangSah() {
        assertEquals(
            listOf(TemaAplikasi.TERANG, TemaAplikasi.GELAP, TemaAplikasi.SISTEM),
            TemaAplikasi.SEMUA,
        )
        assertEquals("mode kembar akan tampil sebagai chip yang sama", 3, TemaAplikasi.SEMUA.distinct().size)
    }

    /** Terang dan gelap harus mengabaikan tema sistem; kalau tidak, pilihannya tidak berpengaruh. */
    @Test
    fun terangDanGelapTidakBergantungTemaSistem() {
        listOf(true, false).forEach { sistem ->
            assertFalse("terang seharusnya selalu terang", TemaAplikasi.gelap(TemaAplikasi.TERANG, sistem))
            assertTrue("gelap seharusnya selalu gelap", TemaAplikasi.gelap(TemaAplikasi.GELAP, sistem))
        }
    }

    @Test
    fun sistemMengikutiTemaSistem() {
        assertTrue(TemaAplikasi.gelap(TemaAplikasi.SISTEM, sistemGelap = true))
        assertFalse(TemaAplikasi.gelap(TemaAplikasi.SISTEM, sistemGelap = false))
    }

    /** Nilai tak dikenal jatuh ke "ikut sistem", sama seperti tema halaman yang jatuh ke `IKUT`. */
    @Test
    fun modeTakDikenalJatuhKeTemaSistem() {
        listOf("", "biru", "SISTEM", "TERANG", "gelap ").forEach { asing ->
            assertEquals(
                "'$asing' seharusnya diperlakukan sebagai ikut sistem",
                TemaAplikasi.gelap(TemaAplikasi.SISTEM, sistemGelap = true),
                TemaAplikasi.gelap(asing, sistemGelap = true),
            )
            assertEquals(
                "'$asing' seharusnya diperlakukan sebagai ikut sistem",
                TemaAplikasi.gelap(TemaAplikasi.SISTEM, sistemGelap = false),
                TemaAplikasi.gelap(asing, sistemGelap = false),
            )
        }
    }

    @Test
    fun akarAplikasiMemakaiPemetaanBersama() {
        val m = mainActivity()
        assertTrue("MainActivity tidak memakai pemetaan bersama", m.contains("TemaAplikasi.gelap("))
        assertFalse("MainActivity masih memetakan mode sendiri", m.contains("\"terang\" ->"))
    }

    @Test
    fun bawaanSelarasAntaraViewModelDanPengaturan() {
        assertTrue("MainViewModel tidak memakai bawaan bersama", mainViewModel().contains("TemaAplikasi.SISTEM"))
        val p = pengaturan()
        assertTrue("layar Pengaturan tidak memakai bawaan bersama", p.contains("TemaAplikasi.SISTEM"))
        assertFalse("layar Pengaturan masih menuliskan bawaan tema sendiri", p.contains("\"sistem\""))
    }

    @Test
    fun layarPengaturanMenawarkanDaftarBersama() {
        val p = pengaturan()
        assertTrue("layar Pengaturan tidak menawarkan daftar mode bersama", p.contains("TemaAplikasi.SEMUA.forEach"))
        assertFalse("layar Pengaturan masih menyusun daftar mode sendiri", p.contains("listOf(\"terang\""))
    }

    /** Aturan bersama harus tetap murni: tanpa Android, agar dapat diuji langsung. */
    @Test
    fun aturanTemaTidakMenyentuhAndroid() {
        assertFalse("TemaAplikasi mengimpor Android sehingga tak dapat diuji di JVM",
            aturan().contains("import android."))
    }
}
