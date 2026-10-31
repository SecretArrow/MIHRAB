package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Uji instrumented tidak boleh memakai API Compose-test yang **usang**.
 *
 * Sejak Compose 1.11 (BOM 2026.06.01), `androidx.compose.ui.test.junit4.createComposeRule` dan
 * `createAndroidComposeRule` ditandai `@Deprecated`, dan kompilasi `assembleDebugAndroidTest`
 * memunculkan peringatan untuk tiap pemakaiannya. Peringatan itu bukan kebisingan yang bisa
 * diabaikan: penggantinya **sudah ada di classpath yang sama** dan berbeda hanya pada dispatcher
 * internalnya, sehingga menundanya berarti menanggung peringatan yang menua bersama setiap kenaikan
 * Compose berikutnya — dan peringatan yang selalu ada berhenti dibaca.
 *
 * Bentuk lama **tidak** diselesaikan dengan `@Suppress("DEPRECATION")`: itu mematikan sinyalnya
 * tanpa memindahkan ujinya, sehingga berkasnya tetap merah saat Compose akhirnya menghapus API lama.
 * Yang diperiksa karena itu adalah **jalur paketnya** (`…junit4` versus `…junit4.v2`) — itulah yang
 * menentukan fungsi mana yang dipanggil, baik lewat baris `import` maupun lewat pemanggilan
 * berpaket penuh.
 *
 * Pencariannya dilakukan pada kode **tanpa komentar dan tanpa isi string**: KDoc yang menjelaskan
 * pola lama memuat tulisan yang sama persis dengan yang dilarang, sehingga penjaga yang longgar
 * justru gagal karena penjelasannya bagus — kekeliruan yang sudah berulang di repo ini.
 *
 * Ruang lingkupnya seluruh `src/androidTest` dan hanya itu: `ui-test-junit4` dideklarasikan sebagai
 * `androidTestImplementation`, jadi tak ada berkas di luar sana yang dapat memakainya.
 */
class UjiInstrumentedTanpaApiUsangTest {

    private fun berkasUji(): List<File> =
        File(KodeSumber.root(), "src/androidTest").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    /**
     * Jalur usang → penggantinya. Ditulis sebagai jalur paket **utuh**, bukan akhiran nama fungsi:
     * yang membedakan usang dan tidak usang adalah paketnya, sedangkan nama fungsinya sama persis —
     * mencocokkan nama saja akan menuduh pemakaian yang sudah benar.
     *
     * Bentuk `…junit4.v2.createComposeRule` tidak memuat `…junit4.createComposeRule`, jadi
     * pencarian potongan di bawah tidak pernah salah menuduh penggantinya sendiri.
     */
    private val usang: Map<String, String> = mapOf(
        "androidx.compose.ui.test.junit4.createComposeRule" to
            "androidx.compose.ui.test.junit4.v2.createComposeRule",
        "androidx.compose.ui.test.junit4.createAndroidComposeRule" to
            "androidx.compose.ui.test.junit4.v2.createAndroidComposeRule",
    )

    /** Kode uji instrumentasi tanpa komentar dan tanpa isi string — prosa tidak ikut diperiksa. */
    private fun kodeBersih(f: File): String =
        KodeSumber.kodeTanpaKomentar(f.readText(), buangString = true)

    @Test
    fun ujiInstrumentedTidakMemakaiApiComposeTestUsang() {
        val berkas = berkasUji()
        assertTrue("berkas uji instrumentasi tidak ditemukan", berkas.isNotEmpty())

        val pelanggar = mutableListOf<String>()
        berkas.forEach { f ->
            val kode = kodeBersih(f)
            usang.forEach { (lama, pengganti) ->
                if (kode.contains(lama)) pelanggar += "${f.name}: `$lama` → pakai `$pengganti`"
            }
        }
        assertTrue(
            "uji instrumented memakai API Compose-test yang usang — pindahkan pemakaiannya ke " +
                "paket `…junit4.v2`, jangan di-`@Suppress`: $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /**
     * Penjaga di atas hanya berarti bila ujinya memang memakai aturan Compose. Bila kelak seluruh
     * `src/androidTest` berhenti memakai `ui-test-junit4`, "tak ada pemakaian usang" akan benar
     * tetapi **hampa** — hijau tanpa memeriksa apa pun. Prasyarat ini membuat keadaan itu terlihat.
     */
    @Test
    fun penggantinyaBenarBenarDipakai() {
        val semua = berkasUji().joinToString("\n") { kodeBersih(it) }
        assertTrue("tak satu pun uji instrumented memakai aturan Compose (`ui-test-junit4`) — " +
            "penjaga pemakaian usang di atas jadi hampa",
            usang.values.any { semua.contains(it) })
    }
}
