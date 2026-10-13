package id.or.kemenag.yasintahlil

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Uji asap UI nyata: meluncurkan aplikasi sungguhan lalu berpindah antar tab bilah bawah,
 * memastikan tiap layar utama benar-benar tampil — bukan sekadar membandingkan konstanta rute.
 *
 * **Nama layar dibaca dari [NamaLayar], tidak dieja di sini.** Dahulu uji ini mengetik sendiri
 * teks tombolnya (`"Yasin"`), dan ketika menu Ibadah mengganti label itu menjadi *"Surat Yasin"*
 * (`NamaLayar.YASIN`) uji ini gagal mencari simpul yang tak ada lagi — padahal aplikasinya benar.
 * Uji yang menyalin nama layar adalah salinan kedua yang dapat basi tanpa galat: ia menguji
 * **ejaan**, bukan navigasi. Karena itu tombol dan tab di sini diklik lewat nama bersama, dan
 * `NamaLayarDiUjiInstrumentedTest` memaku aturannya.
 *
 * **Izin notifikasi diberikan lebih dulu, sebelum Activity diluncurkan.** `MainActivity.onCreate`
 * meminta `POST_NOTIFICATIONS` (sejak API 33). Pada perangkat yang izinnya belum diberikan,
 * permintaan itu memunculkan **dialog izin sistem** tepat saat Activity start, dan uji ini
 * meluncurkan Activity sungguhan — sehingga dialog itulah yang menerima fokus, bukan jendela
 * aplikasi, dan ComposeTestRule gagal dengan *"No compose hierarchies found in the app"*: galat
 * yang menyesatkan, sebab aplikasinya sendiri sehat.
 *
 * Buktinya dari run `main` 38466684400 (emulator API 36): seluruh 40 uji lain — yang memanggil
 * `setContent` langsung tanpa meluncurkan Activity — lolos, dan hanya dua uji kelas inilah yang
 * merah. Cabang izin itu adalah satu-satunya jalur kode yang bergantung API >= 33 dan hanya
 * dilalui uji kelas ini; di emulator lama (API 29) ia tak pernah jalan, sehingga masalahnya tak
 * terlihat sampai `api-level` dinaikkan. Bila ternyata sebabnya lain, `.github/scripts/
 * e2e-instrumented.sh` kini membuang logcat dan laporan uji pada jalur kegagalan, jadi sebabnya
 * akan terbaca alih-alih ditebak.
 *
 * Ini bukan menyembunyikan kegagalan aplikasi: yang diuji kelas ini adalah navigasi, sedangkan
 * alur permintaan izin diuji `KiblatLocationPermissionTest`.
 */
@RunWith(AndroidJUnit4::class)
class NavSmokeTest {

    /**
     * Aturan Compose, **tanpa** anotasi `@Rule` — ia dipasang lewat [rantai] di bawah. Memberinya
     * `@Rule` sekaligus akan membuatnya diterapkan dua kali (sekali sendiri, sekali di dalam
     * rantai), sehingga Activity-nya diluncurkan dua kali.
     */
    val rule = createAndroidComposeRule<MainActivity>()

    /** Memberi `POST_NOTIFICATIONS` sebelum Activity diluncurkan, supaya tak ada dialog sistem. */
    private val izinNotifikasi = object : ExternalResource() {
        override fun before() {
            if (Build.VERSION.SDK_INT < 33) return
            val inst = InstrumentationRegistry.getInstrumentation()
            // Bila pemberian izin gagal (mis. perangkat menolak lewat kebijakan), uji tetap
            // dijalankan seperti sebelumnya — kegagalannya akan terlihat, bukan disembunyikan.
            runCatching {
                inst.uiAutomation.grantRuntimePermission(
                    inst.targetContext.packageName,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }
    }

    /**
     * Rantai aturan: izin diberikan di aturan **luar**, sehingga ia berjalan sebelum aturan
     * Compose (aturan dalam) meluncurkan Activity-nya.
     */
    @get:Rule
    val rantai: RuleChain = RuleChain.outerRule(izinNotifikasi).around(rule)

    @Test
    fun bottomNavigation_switchesBetweenMainScreens() {
        // Beranda tampil lebih dulu.
        rule.onNodeWithText("Assalamu'alaikum").assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.IBADAH).performClick()
        rule.onNodeWithText("Menu ${NamaLayar.IBADAH}").assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.ALAT).performClick()
        rule.onNodeWithText("Menu ${NamaLayar.ALAT}").assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.PENGATURAN).performClick()
        // Judul bagian Tentang ada di bagian bawah layar Pengaturan yang bisa digulir — gulir dulu.
        rule.onNodeWithText(NamaLayar.TENTANG).performScrollTo().assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.BERANDA).performClick()
        rule.onNodeWithText("Assalamu'alaikum").assertIsDisplayed()
    }

    @Test
    fun opensYasinFromIbadahMenu() {
        rule.onNodeWithText(NamaLayar.IBADAH).performClick()
        rule.onNodeWithText(NamaLayar.YASIN).performClick()
        // Penanda khas layar Yasin.
        rule.onNodeWithText("Ke ayat").assertIsDisplayed()
    }
}
