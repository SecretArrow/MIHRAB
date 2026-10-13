package id.or.kemenag.yasintahlil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import org.junit.Rule
import org.junit.Test
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
 */
@RunWith(AndroidJUnit4::class)
class NavSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_switchesBetweenMainScreens() {
        // Beranda tampil lebih dulu.
        rule.onNodeWithText("Assalamu'alaikum").assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.IBADAH).performClick()
        rule.onNodeWithText("Menu ${NamaLayar.IBADAH}").assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.ALAT).performClick()
        rule.onNodeWithText("Menu ${NamaLayar.ALAT}").assertIsDisplayed()

        rule.onNodeWithText(NamaLayar.PENGATURAN).performClick()
        // "Tentang & sumber" ada di bagian bawah layar Pengaturan yang bisa digulir — gulir dulu.
        rule.onNodeWithText("Tentang & sumber").performScrollTo().assertIsDisplayed()

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
