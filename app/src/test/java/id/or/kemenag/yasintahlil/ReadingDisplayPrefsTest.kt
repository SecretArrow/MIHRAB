package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Preferensi tampilan bacaan ("Arab / Latin / Terjemahan") harus benar-benar berpengaruh.
 * Sebelumnya kunci `SHOW_ARAB` hanya tersimpan tanpa mengubah layar mana pun — uji ini
 * menjaga agar setiap pemanggilan `BacaanCard`/`ArabText` di layar fitur menyertakan
 * `showArab`, sehingga tombol di Pengaturan tidak menjadi kontrol mati lagi.
 */
class ReadingDisplayPrefsTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun featureSources(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun storeSource(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt").readText()

    /**
     * Potongan sumber tiap pemanggilan `nama(` dengan tanda kurung yang seimbang, diawali
     * teks pada baris yang sama sebelum pemanggilan — agar penjaga di luar tanda kurung
     * (mis. `if (s.showArab && arab.isNotBlank()) ArabText(...)`) ikut terbaca.
     */
    private fun callSites(src: String, nama: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (true) {
            val at = src.indexOf("$nama(", i)
            if (at < 0) break
            var depth = 0
            var j = at + nama.length
            while (j < src.length) {
                when (src[j]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) { j++; break }
                    }
                }
                j++
            }
            val barisMulai = src.lastIndexOf('\n', at) + 1
            out += src.substring(barisMulai, minOf(j, src.length))
            i = maxOf(j, at + nama.length + 1)
        }
        return out
    }

    @Test
    fun setiap_pemanggilan_BacaanCard_menghormati_showArab() {
        val offenders = featureSources().flatMap { f ->
            callSites(f.readText(), "BacaanCard")
                .filterNot { it.contains("showArab") }
                .map { "${f.name}: ${it.replace(Regex("\\s+"), " ").take(80)}" }
        }
        assertTrue(
            "Ada pemanggilan BacaanCard tanpa showArab sehingga chip \"Arab\" di Pengaturan " +
                "tidak berpengaruh:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun ArabText_langsung_juga_dihormati_showArab() {
        val offenders = featureSources().flatMap { f ->
            callSites(f.readText(), "ArabText")
                .filterNot { it.contains("showArab") }
                .map { "${f.name}: ${it.replace(Regex("\\s+"), " ").take(80)}" }
        }
        assertTrue(
            "Ada ArabText yang tampil tanpa memeriksa showArab:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun kunci_SHOW_ARAB_dideklarasikan_dan_dibaca() {
        val store = storeSource()
        assertTrue("Store.SHOW_ARAB tidak dideklarasikan",
            store.contains("val SHOW_ARAB = booleanPreferencesKey"))
        // Harus dibaca di luar Store.kt, jika tidak chip "Arab" kembali menjadi kontrol mati.
        val pembaca = featureSources().filter { it.readText().contains("Store.SHOW_ARAB") }
        assertTrue("Tidak ada layar fitur yang membaca Store.SHOW_ARAB", pembaca.isNotEmpty())
    }
}
