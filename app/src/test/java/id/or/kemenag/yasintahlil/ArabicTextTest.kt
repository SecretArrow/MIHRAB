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

    private companion object {
        /** Fathatan..sukun + alef khanjariyah dan tanda maddah. */
        val HARAKAT: Set<Char> = (0x064B..0x0652).map { it.toChar() }.toSet() +
            listOf(0x0670, 0x0653, 0x0654, 0x0655, 0x0656).map { it.toChar() }

        val ARABIC_RUN = Regex("[\\u0600-\\u06FF\\u0750-\\u077F]+")
    }

    /** Kata Arab (>= 3 huruf) tanpa satu pun tanda harakat. */
    private fun unvocalized(text: String): List<String> =
        ARABIC_RUN.findAll(text).map { it.value }.filter { run ->
            val letters = run.count { c ->
                c.code in 0x0621..0x064A || c.code in 0x0671..0x06D3
            }
            letters >= 3 && run.none { it in HARAKAT }
        }.toList()

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

        val arabic = Regex("[\\u0600-\\u06FF\\u0750-\\u077F]")
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
