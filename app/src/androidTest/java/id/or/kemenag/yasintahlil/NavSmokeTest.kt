package id.or.kemenag.yasintahlil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uji asap UI nyata: meluncurkan aplikasi sungguhan lalu berpindah antar tab bilah bawah,
 * memastikan tiap layar utama benar-benar tampil — bukan sekadar membandingkan konstanta rute.
 */
@RunWith(AndroidJUnit4::class)
class NavSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_switchesBetweenMainScreens() {
        // Beranda tampil lebih dulu.
        rule.onNodeWithText("Assalamu'alaikum").assertIsDisplayed()

        rule.onNodeWithText("Ibadah").performClick()
        rule.onNodeWithText("Menu Ibadah").assertIsDisplayed()

        rule.onNodeWithText("Alat").performClick()
        rule.onNodeWithText("Menu Alat").assertIsDisplayed()

        rule.onNodeWithText("Pengaturan").performClick()
        // "Tentang & sumber" ada di bagian bawah layar Pengaturan yang bisa digulir — gulir dulu.
        rule.onNodeWithText("Tentang & sumber").performScrollTo().assertIsDisplayed()

        rule.onNodeWithText("Beranda").performClick()
        rule.onNodeWithText("Assalamu'alaikum").assertIsDisplayed()
    }

    @Test
    fun opensYasinFromIbadahMenu() {
        rule.onNodeWithText("Ibadah").performClick()
        rule.onNodeWithText("Yasin (83 ayat, MSI Kemenag)").performClick()
        // Penanda khas layar Yasin.
        rule.onNodeWithText("Ke ayat").assertIsDisplayed()
    }
}
