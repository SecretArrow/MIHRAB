package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pengaturan Aplikasi" meminta pengaturan *"Ukuran teks Arab dan Latin"*, dan
 * §"Pustaka Doa dan Dzikir" meminta setiap bacaan mendukung *"teks Arab, transliterasi, terjemahan"*.
 *
 * Sebelumnya hanya layar Yasin yang membaca `Store.LATIN_SIZE`/`Store.LINE_HEIGHT`; seluruh layar
 * bacaan lain menuliskan angka tetap (`15f`/`16f`) pada panggilan `BacaanCard`. Akibatnya slider
 * "Ukuran Latin" di Pengaturan — dan label sheet yang berbunyi "berlaku untuk semua bacaan" —
 * hanya berpengaruh di satu layar. Uji ini menjaga agar ukuran Latin dan jarak antarbaris tetap
 * menjadi preferensi **global**: ditulis di satu tempat (sheet) dan dibaca `BacaanCard` sendiri.
 */
class UkuranBacaanGlobalTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun uiKit(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt").readText()

    private fun yasinVm(): String = File(
        root(),
        "src/main/java/id/or/kemenag/yasintahlil/feature/yasin/YasinViewModel.kt",
    ).readText()

    private fun sumberFitur(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Badan (di dalam `{ … }`) sebuah deklarasi fungsi, dicari dengan kurung yang seimbang. */
    private fun badanFungsi(src: String, nama: String): String {
        val deklarasi = src.indexOf("fun $nama(")
        if (deklarasi < 0) return ""
        var depth = 0
        var i = deklarasi + "fun $nama".length
        while (i < src.length) {
            when (src[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) { i++; break }
                }
            }
            i++
        }
        while (i < src.length && src[i] != '{') i++
        if (i >= src.length) return ""
        val mulai = i
        depth = 0
        while (i < src.length) {
            when (src[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) { i++; break }
                }
            }
            i++
        }
        return src.substring(mulai, minOf(i, src.length))
    }

    /** Seluruh daftar argumen setiap pemanggilan `nama(...)`, dicari dengan kurung yang seimbang. */
    private fun daftarArgumen(src: String, nama: String): List<String> {
        val hasil = mutableListOf<String>()
        var i = src.indexOf("$nama(")
        while (i >= 0) {
            val start = src.indexOf('(', i)
            var depth = 0
            for (j in start until src.length) {
                when (src[j]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) { hasil += src.substring(start + 1, j); break }
                    }
                }
            }
            i = src.indexOf("$nama(", i + 1)
        }
        return hasil
    }

    @Test
    fun kartuBacaanMembacaUkuranGlobalSaatTidakDioper() {
        val badan = badanFungsi(uiKit(), "BacaanCard")
        assertTrue("BacaanCard tidak membaca Store.LATIN_SIZE — ukuran Latin tak global",
            badan.contains("Store.LATIN_SIZE"))
        assertTrue("BacaanCard tidak membaca Store.LINE_HEIGHT — jarak antarbaris tak global",
            badan.contains("Store.LINE_HEIGHT"))
        assertTrue("BacaanCard tidak memakai preferensi global saat nilainya tidak dioper",
            badan.contains("latinSize ?: latinGlobal"))
        assertTrue("BacaanCard tidak memakai jarak antarbaris global saat nilainya tidak dioper",
            badan.contains("lineHeightFactor ?: tinggiGlobal"))
    }

    @Test
    fun pengaturanMenulisUkuranGlobal() {
        // Satu-satunya penulis preferensi ini kini layar Pengaturan (dahulu bottom sheet per
        // layar bacaan). Ia harus tetap menulis kedua kunci global itu sendiri.
        val pengaturan = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
        ).readText()
        assertTrue("Pengaturan tidak menyediakan slider ukuran Latin",
            pengaturan.contains("UkuranBacaan.LATIN"))
        assertTrue("Pengaturan tidak menyediakan slider jarak antarbaris",
            pengaturan.contains("UkuranBacaan.JARAK"))
        assertTrue("Pengaturan tidak menulis Store.LATIN_SIZE",
            pengaturan.contains("store.set(Store.LATIN_SIZE"))
        assertTrue("Pengaturan tidak menulis Store.LINE_HEIGHT",
            pengaturan.contains("store.set(Store.LINE_HEIGHT"))
    }

    /** Angka tetap pada panggilan `BacaanCard` berarti layar itu mengabaikan pilihan pengguna. */
    @Test
    fun tidakAdaUkuranLatinAngkaTetapDiLayar() {
        val pelanggar = mutableListOf<String>()
        sumberFitur().forEach { f ->
            daftarArgumen(f.readText(), "BacaanCard").forEach { arg ->
                val angka = Regex("""(?<![\w.])\d+(?:\.\d+)?f(?![\w])""").findAll(arg).map { it.value }
                angka.forEach { pelanggar += "${f.name}: $it" }
            }
        }
        assertTrue(
            "Ukuran ditulis sebagai angka tetap di layar, bukan dari preferensi global: $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /** Layar tidak lagi mengoper ukuran Latin ke sheet — sheet memilikinya sendiri. */
    @Test
    fun layarTidakMengoperUkuranLatinKeSheet() {
        val pelanggar = sumberFitur().filter {
            it.readText().contains("onLatinSize") || it.readText().contains("onLineHeight")
        }.map { it.name }
        assertTrue("Layar masih mengoper ukuran Latin/jarak antarbaris ke sheet: $pelanggar",
            pelanggar.isEmpty())
    }

    /** Satu sumber kebenaran: hanya sheet (dan layar Pengaturan) yang memiliki preferensi ini. */
    @Test
    fun viewModelYasinTidakLagiMenyimpanUkuranLatin() {
        val s = yasinVm()
        assertFalse("YasinViewModel masih menyimpan latinSize — dua sumber kebenaran",
            s.contains("Store.LATIN_SIZE"))
        assertFalse("YasinViewModel masih menyimpan lineHeight — dua sumber kebenaran",
            s.contains("Store.LINE_HEIGHT"))
    }
}
