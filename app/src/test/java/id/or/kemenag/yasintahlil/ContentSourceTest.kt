package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga aturan SOURCES.md: setiap file konten wajib mencantumkan `sumber_url`
 * dengan domain resmi. Dibaca langsung dari disk agar jalan tanpa Robolectric.
 */
class ContentSourceTest {

    private fun contentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Direktori assets/content tidak ditemukan dari ${File(".").absolutePath}")
    }

    @Test
    fun everyContentFileDeclaresOfficialSource() {
        val files = contentDir().listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("Tidak ada file konten ditemukan", files.isNotEmpty())

        val problems = mutableListOf<String>()
        files.forEach { f ->
            val text = f.readText()
            val url = Regex("\"sumber_url\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            when {
                url == null -> problems += "${f.name}: tanpa sumber_url"
                !ContentValidator.isOfficialSource(url) -> problems += "${f.name}: domain tidak resmi ($url)"
            }
        }
        assertTrue("Pelanggaran sumber resmi: $problems", problems.isEmpty())
    }

    /** Tidak boleh ada teks instruksi yang menyamar sebagai lafaz Arab. */
    @Test
    fun arabicFieldsAreNotPlaceholders() {
        val badMarkers = listOf("lafaz niat per sholat", "... (sesuai", "TODO", "Lorem")
        val files = contentDir().listFiles { f -> f.extension == "json" }.orEmpty()
        val problems = mutableListOf<String>()
        files.forEach { f ->
            val text = f.readText()
            badMarkers.forEach { m -> if (text.contains(m)) problems += "${f.name}: '$m'" }
        }
        assertTrue("Placeholder terdeteksi: $problems", problems.isEmpty())
    }

    /**
     * wudhu.json sengaja tanpa teks Arab (lafaz menunggu verifikasi sumber resmi). Uji ini
     * menjaga agar tidak ada yang diam-diam menyisipkan teks Arab hasil karangan ke berkas itu.
     */
    @Test
    fun wudhuContentHasNoFabricatedArabic() {
        val f = File(contentDir(), "wudhu.json")
        assertTrue("wudhu.json tidak ditemukan", f.isFile)
        val text = f.readText()
        val arabic = Regex("[\\u0600-\\u06FF]").find(text)
        assertTrue("wudhu.json memuat teks Arab tanpa sumber: '${arabic?.value}'", arabic == null)
        assertTrue("wudhu.json kehilangan bagian tayamum", text.contains("tayamum"))
        assertTrue("wudhu.json kehilangan bagian pembatal", text.contains("pembatal"))
    }
}
