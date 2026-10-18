package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga dua invarian teks Arab:
 *
 * 1. **Berharakat.** Seluruh teks Arab di assets harus lengkap barisnya agar dapat dibaca
 *    pemula. Kata Arab (>= 3 huruf) yang sama sekali tanpa harakat hampir pasti teks mentah
 *    yang belum divokalkan — sebelumnya ada di `adzan.json` (`الصلاة خير من النوم`).
 * 2. **Tidak hardcoded di Kotlin.** Teks Arab hanya boleh berasal dari assets (SOURCES.md),
 *    sehingga layar tidak dapat menampilkan lafaz yang tak terdokumentasi sumbernya.
 */
class ArabicTextTest {

    private fun contentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Direktori assets/content tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    /**
     * Kata Arab tanpa harakat, memakai aturan bersama [TeksArab].
     *
     * Uji ini dahulu mengeja sendiri rentang Unicode-nya — blok Arab, rentang huruf, tanda harakat,
     * dan panjang kata minimum — persis seperti `BundelKonten` dan `tools/validate_content.py`.
     * Ketiganya bernilai sama tetapi tak ada yang membandingkannya, sehingga aturan yang diperbaiki
     * di satu tempat tetap salah di dua tempat lainnya. Kini ketiganya membaca [TeksArab].
     *
     * Yang tetap khas uji ini hanyalah **bagian mana yang diperiksa**: setiap nilai string JSON,
     * bukan hanya field `arab`/`latin`.
     */
    private fun unvocalized(text: String): List<String> = TeksArab.tanpaHarakat(text)

    @Test
    fun semuaTeksArabDiAssetsBerharakat() {
        val files = contentDir().listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("Tidak ada file konten ditemukan", files.isNotEmpty())

        val problems = mutableListOf<String>()
        files.forEach { f ->
            val text = f.readText()
            // Ambil hanya nilai string JSON agar path/URL tidak ikut dinilai.
            Regex("\"([^\"]*)\"").findAll(text).forEach { m ->
                val v = m.groupValues[1]
                unvocalized(v).takeIf { it.isNotEmpty() }?.let {
                    problems += "${f.name}: '$v' -> ${it.joinToString(" ")}"
                }
            }
        }
        assertTrue("Teks Arab tanpa harakat:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun tidakAdaTeksArabHardcodedDiLayar() {
        val screens = File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("Tidak ada sumber Kotlin ditemukan", screens.isNotEmpty())

        val arabic = TeksArab.regexArab
        val problems = screens.mapNotNull { f ->
            arabic.find(f.readText())?.let { "${f.name}: '${it.value}'" }
        }
        assertTrue(
            "Teks Arab hardcoded di kode — pindahkan ke assets/content agar sumbernya tercatat:\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }
}
