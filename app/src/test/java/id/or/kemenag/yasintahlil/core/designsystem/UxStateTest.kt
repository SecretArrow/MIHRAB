package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` meminta: *"Gunakan bottom sheet untuk pengaturan singkat"*, *"Berikan skeleton loading
 * atau indikator proses saat memuat data"*, *"Sediakan tampilan kosong, pesan kesalahan, dan tombol
 * mencoba kembali"*, serta *"Hindari tombol dekoratif yang tidak bekerja"*.
 *
 * Uji ini menjaga agar keempat komponen keadaan tetap ada **dan** benar-benar dipakai, agar
 * pengaturan tampilan bacaan benar-benar dibuka lewat bottom sheet (bukan chip yang berserakan di
 * badan layar), dan agar tak ada tombol yang tampak bisa ditekan tetapi tak melakukan apa pun.
 */
class UxStateTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun featureSources(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun uiKit(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt").readText()

    private fun semuaSumberUi(): List<File> = featureSources() +
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Badan (di dalam `{ … }`) sebuah deklarasi fungsi, dicari dengan kurung yang seimbang. */
    private fun badanFungsi(src: String, nama: String): String {
        val deklarasi = src.indexOf("fun $nama(")
        if (deklarasi < 0) return ""
        var depth = 0
        var i = deklarasi + "fun $nama".length
        // Lewati daftar parameter.
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
        // Lewati tipe kembalian sampai kurung kurawal badan fungsi.
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

    @Test
    fun komponen_keadaan_dideklarasikan() {
        val ui = uiKit()
        listOf("EmptyState", "ErrorState", "LoadingState", "SkeletonCard", "BacaanSettingsSheet")
            .forEach { nama ->
                assertTrue("UiKit.kt tidak mendeklarasikan $nama",
                    Regex("""fun\s+$nama\s*\(""").containsMatchIn(ui))
            }
    }

    @Test
    fun komponen_keadaan_dipakai_layar() {
        val sumber = featureSources().map { it.readText() }
        listOf("EmptyState", "ErrorState", "LoadingState", "SkeletonCard").forEach { nama ->
            assertTrue("Tidak ada layar fitur yang memakai $nama — komponen mati",
                sumber.any { it.contains("$nama(") })
        }
    }

    /**
     * Layar yang menampilkan bacaan harus mengatur tampilannya lewat bottom sheet: setiap berkas
     * yang memanggil `BacaanCard` juga harus memanggil `BacaanSettingsSheet` **dan** menyediakan
     * tombol pembukanya — supaya sheet-nya bukan kode mati yang tak bisa dibuka pengguna.
     *
     * Label tombolnya datang dari kosakata bersama `NamaAntarmuka`, bukan literal yang dieja di
     * layar: tombol harus menyebut apa yang akan terbuka, sama dengan judul sheet-nya.
     */
    @Test
    fun pengaturan_bacaan_lewat_bottom_sheet() {
        val pelanggar = featureSources().filter { it.readText().contains("BacaanCard(") }
            .filterNot { f ->
                val t = f.readText()
                t.contains("BacaanSettingsSheet(") && t.contains("NamaAntarmuka.PENGATURAN_BACAAN")
            }
            .map { it.name }
        assertTrue("Layar bacaan tanpa bottom sheet pengaturan: $pelanggar", pelanggar.isEmpty())
    }

    /** Rentang ukuran yang sah hanya ditulis sekali, di `UkuranBacaan` — bukan angka lepas per layar. */
    @Test
    fun rentang_ukuran_ditulis_sekali() {
        val aturan = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/core/util/UkuranBacaan.kt",
        ).readText()
        listOf("16f..40f", "12f..24f", "1.4f..2.6f").forEach { r ->
            assertTrue("Rentang $r tidak ada di aturan bersama UkuranBacaan", aturan.contains(r))
        }
        val sheet = badanFungsi(uiKit(), "BacaanSettingsSheet")
        listOf("UkuranBacaan.ARAB", "UkuranBacaan.LATIN", "UkuranBacaan.JARAK").forEach { r ->
            assertTrue("$r tidak dipakai sheet pengaturan bacaan", sheet.contains(r))
        }
        val lepas = featureSources().filter { f ->
            val t = f.readText()
            listOf("16f..40f", "12f..24f", "1.4f..2.6f").any { t.contains(it) }
        }.map { it.name }
        assertTrue("Angka rentang ditulis ulang di layar (bukan dari aturan bersama): $lepas",
            lepas.isEmpty())
    }

    /**
     * Tidak ada tombol dekoratif: `onClick = {}` hanya boleh dipakai pada kontrol yang memang
     * dimatikan (`enabled = false`) sebagai penanda status, bukan pada tombol yang tampak aktif.
     */
    @Test
    fun tidak_ada_tombol_dekoratif() {
        val pelanggar = mutableListOf<String>()
        semuaSumberUi().forEach { f ->
            val t = f.readText()
            Regex("""onClick\s*=\s*\{\s*\}""").findAll(t).forEach { m ->
                val jendela = t.substring(maxOf(0, m.range.first - 240), minOf(t.length, m.range.last + 240))
                if (!jendela.contains("enabled = false")) {
                    val baris = t.take(m.range.first).count { it == '\n' } + 1
                    pelanggar += "${f.name}:$baris"
                }
            }
        }
        assertTrue("Tombol yang tampak aktif tetapi tak melakukan apa pun: $pelanggar",
            pelanggar.isEmpty())
    }
}
