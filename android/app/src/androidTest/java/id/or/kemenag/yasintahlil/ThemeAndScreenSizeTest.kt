package id.or.kemenag.yasintahlil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.or.kemenag.yasintahlil.core.designsystem.BacaanCard
import id.or.kemenag.yasintahlil.core.designsystem.IslamiTheme
import id.or.kemenag.yasintahlil.feature.kiblat.KiblatMap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * plan.txt: "Pengujian tema terang dan gelap" + "Pengujian pada beberapa ukuran layar."
 *
 * Kartu bacaan adalah komponen inti seluruh modul baca; ia harus tetap tampil utuh pada tema
 * terang maupun gelap, dan pada lebar layar ponsel kecil maupun tablet.
 * Peta kiblat diuji pula karena ia menjadi acuan arah saat perangkat tidak punya kompas.
 */
@RunWith(AndroidJUnit4::class)
class ThemeAndScreenSizeTest {

    // Diimpor dari `…junit4.v2`: bentuk lama `createComposeRule` ditandai usang sejak Compose 1.11.
    // Tipe yang dikembalikan sama; yang berubah hanya dispatcher internalnya, dan uji di kelas ini
    // hanya `setContent` lalu memeriksa simpul, jadi tak bergantung pada eksekusi segera.
    @get:Rule
    val rule = createComposeRule()

    /** Menampilkan kartu bacaan pada lebar layar tertentu (dp) dan tema terang/gelap. */
    private fun showBacaan(dark: Boolean, width: Int) {
        rule.setContent {
            IslamiTheme(dark = dark) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.requiredSize(width.dp, 360.dp)) {
                        BacaanCard(
                            id = "uji:1", judul = "Bacaan uji",
                            arab = "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ",
                            latin = "Bismillahir-rahmanir-rahim",
                            terjemahan = "Dengan nama Allah Yang Maha Pengasih lagi Maha Penyayang",
                            showLatin = true, showTrans = true,
                            arabSize = 24f, latinSize = 16f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun temaTerangMenampilkanBacaan() {
        showBacaan(dark = false, width = 320)
        rule.onNodeWithText("Bacaan uji").assertIsDisplayed()
    }

    @Test
    fun temaGelapMenampilkanBacaan() {
        showBacaan(dark = true, width = 320)
        rule.onNodeWithText("Bacaan uji").assertIsDisplayed()
    }

    @Test
    fun layarPonselKecil() {
        showBacaan(dark = false, width = 320)
        rule.onNodeWithText("Bacaan uji").assertIsDisplayed()
    }

    @Test
    fun layarTablet() {
        showBacaan(dark = false, width = 800)
        rule.onNodeWithText("Bacaan uji").assertIsDisplayed()
    }

    /** Peta kiblat offline harus dapat digambar (acuan arah bila kompas tidak tersedia). */
    @Test
    fun petaKiblatDapatDigambar() {
        rule.setContent {
            IslamiTheme(dark = false) {
                Box(Modifier.fillMaxSize()) {
                    KiblatMap(lat = -6.2, lon = 106.8, bearing = 295.0)
                }
            }
        }
        rule.onNodeWithContentDescription("Peta arah kiblat", substring = true).assertIsDisplayed()
    }
}
