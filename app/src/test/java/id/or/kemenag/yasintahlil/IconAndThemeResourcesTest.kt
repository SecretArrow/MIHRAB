package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga identitas visual tetap utuh: ikon peluncur, tema jendela terang/gelap, dan
 * keselarasan palet XML dengan palet Compose (Palette). Tanpa uji ini, warna di
 * colors.xml dan Theme.kt dapat menyimpang diam-diam.
 */
class IconAndThemeResourcesTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun res(path: String): File = File(root(), "src/main/res/$path")

    private fun manifest(): String = File(root(), "src/main/AndroidManifest.xml").readText()

    private fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    private fun colorFrom(xml: String, name: String): String? =
        Regex("<color name=\"$name\">\\s*(#[0-9A-Fa-f]{6})\\s*</color>").find(xml)?.groupValues?.get(1)

    @Test
    fun peluncur_memakai_ikon_adaptif() {
        assertTrue("ic_launcher.xml (adaptif) tidak ada", res("mipmap-anydpi-v26/ic_launcher.xml").isFile)
        assertTrue("ic_launcher_round.xml (adaptif) tidak ada", res("mipmap-anydpi-v26/ic_launcher_round.xml").isFile)
        assertTrue("ic_launcher_foreground tidak ada", res("drawable/ic_launcher_foreground.xml").isFile)
        assertTrue("ic_launcher_background tidak ada", res("drawable/ic_launcher_background.xml").isFile)

        val adaptive = res("mipmap-anydpi-v26/ic_launcher.xml").readText()
        assertTrue("Ikon adaptif tanpa layer background", adaptive.contains("android:drawable=\"@drawable/ic_launcher_background\""))
        assertTrue("Ikon adaptif tanpa layer foreground", adaptive.contains("android:drawable=\"@drawable/ic_launcher_foreground\""))

        val m = manifest()
        assertTrue("Manifes tanpa android:icon", m.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue("Manifes tanpa android:roundIcon", m.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test
    fun tema_jendela_ada_untuk_terang_dan_gelap() {
        assertTrue("values/themes.xml tidak ada", res("values/themes.xml").isFile)
        assertTrue("values-night/themes.xml tidak ada (kedip putih saat mode gelap)", res("values-night/themes.xml").isFile)

        val m = manifest()
        assertTrue("Manifes belum memakai tema aplikasi", m.contains("android:theme=\"@style/Theme.YasinTahlil\""))

        val terang = res("values/themes.xml").readText()
        val gelap = res("values-night/themes.xml").readText()
        assertTrue("Tema terang tanpa windowBackground", terang.contains("android:windowBackground"))
        assertTrue("Tema gelap tanpa windowBackground", gelap.contains("android:windowBackground"))
        // Latar gelap harus berbeda dari terang, jika tidak kedipan tetap terjadi.
        assertTrue("Latar tema gelap sama dengan terang",
            !gelap.contains("@color/gading"))
    }

    @Test
    fun label_memakai_string_resource() {
        assertTrue("Manifes masih memakai label literal",
            manifest().contains("android:label=\"@string/app_name\""))
        assertTrue("app_name tidak ada di strings.xml",
            res("values/strings.xml").readText().contains("name=\"app_name\""))
    }

    @Test
    fun palet_xml_selaras_dengan_palet_compose() {
        val xml = res("values/colors.xml").readText()
        assertEquals("zamrud tidak selaras", hex(Palette.ZAMRUD), colorFrom(xml, "zamrud"))
        assertEquals("gading tidak selaras", hex(Palette.GADING), colorFrom(xml, "gading"))
        assertEquals("latar_gelap tidak selaras", hex(Palette.DARK_BG), colorFrom(xml, "latar_gelap"))
    }
}
