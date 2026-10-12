package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Setiap dependensi uji instrumentasi harus benar-benar dipakai.
 *
 * Proyek ini sudah pernah menyimpan deklarasi mati di kode produksi (Hilt, Room, dan WorkManager
 * sempat dideklarasikan sebelum satu pun kelasnya dipakai). Dependensi uji pun sama: `espresso-core`,
 * `uiautomator`, dan `kotlinx-coroutines-test` dulu dideklarasikan di `androidTestImplementation`
 * padahal tidak satu pun kelasnya diimpor berkas di `src/androidTest` — ketiganya hanya menambah
 * ukuran APK uji dan waktu `e2e.yml` tanpa memberi satu pun kemampuan. `plan.txt` menutup dengan
 * *"Semua fitur ... harus dapat diuji"*; deklarasi yang tidak dipakai bukan bagian dari itu.
 *
 * Aturannya satu untuk semua: alias `libs.*` yang muncul di `androidTestImplementation(...)` harus
 * punya paket yang benar-benar diimpor uji instrumentasi. Alias baru tanpa padanan paket
 * menggagalkan uji terakhir, sehingga peta di bawah tidak dapat menua diam-diam.
 *
 * Catatan pemeriksaan: `espresso-core` dan `kotlinx-coroutines-test` **tetap ada** di classpath uji
 * karena `androidx.compose.ui:ui-test-junit4` menariknya sendiri (diperiksa dari POM-nya), jadi
 * ketiadaan deklarasi eksplisit di sini bukan berarti keduanya hilang.
 */
class DependensiUjiTerpakaiTest {

    /**
     * Alias katalog → paket yang diimpor uji instrumentasi bila dependensi itu dipakai.
     *
     * Ditulis tangan, bukan diturunkan dari `group` di katalog: paket Kotlin tidak selalu sama
     * dengan `group`. `org.jetbrains.kotlinx:kotlinx-coroutines-test` berpaket
     * `kotlinx.coroutines`, dan `androidx.work:work-testing` berpaket `androidx.work.testing`
     * (bukan `androidx.work.work.testing`).
     */
    private val paket = mapOf(
        "androidx.test.junit" to "androidx.test.ext.junit",   // AndroidJUnit4
        "androidx.test.core" to "androidx.test.core",         // ApplicationProvider
        "androidx.work.testing" to "androidx.work.testing",   // WorkManagerTestInitHelper
        "compose.ui.test.junit4" to "androidx.compose.ui.test", // createComposeRule
        "kotlinx.coroutines.test" to "kotlinx.coroutines.test", // runTest — kini lewat transitif
        // Ketiga alias di bawah tidak lagi dideklarasikan (dihapus karena tak dipakai), tetapi
        // paketnya tetap dicatat: begitu ada yang menambahkannya kembali ke build.gradle.kts,
        // uji di atas langsung menilainya, bukan membiarkannya lolos tanpa pemeriksaan.
        "espresso" to "androidx.test.espresso",
        "uiautomator" to "androidx.test.uiautomator",
    )

    private fun build(): String = File(KodeSumber.root(), "build.gradle.kts").readText()

    private fun berkasUji(): List<File> =
        File(KodeSumber.root(), "src/androidTest").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    /**
     * Alias pada baris `androidTestImplementation(libs.…)`.
     *
     * `androidTestImplementation(platform(libs.…))` sengaja tidak ikut: itu BOM, bukan pustaka yang
     * kelasnya diimpor uji.
     */
    private fun dideklarasikan(): List<String> =
        Regex("""androidTestImplementation\(libs\.([\w.]+)\)""").findAll(build())
            .map { it.groupValues[1] }
            .toList()

    /** Semua `import …` pada uji instrumentasi, dengan komentar dibuang lebih dulu. */
    private fun impor(): List<String> = berkasUji().flatMap { f ->
        KodeSumber.kodeTanpaKomentar(f.readText())
            .lines().filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").trim() }
    }

    @Test
    fun setiapDependensiUjiDipakai() {
        val berkas = berkasUji()
        assertTrue("berkas uji instrumentasi tidak ditemukan", berkas.isNotEmpty())
        val semua = impor()
        val takTerpakai = dideklarasikan().filter { alias ->
            val p = paket[alias] ?: return@filter false
            semua.none { it == p || it.startsWith("$p.") }
        }
        assertTrue(
            "dependensi uji dideklarasikan di androidTestImplementation tetapi tidak ada berkas " +
                "src/androidTest yang memakainya: $takTerpakai — hapus deklarasinya, atau pakai " +
                "kelasnya bila memang dibutuhkan",
            takTerpakai.isEmpty(),
        )
    }

    @Test
    fun setiapAliasUjiPunyaPadananPaket() {
        val takDikenal = dideklarasikan().filterNot { it in paket }
        assertTrue(
            "alias uji tanpa padanan paket di `paket` (tambahkan agar ikut dijaga): $takDikenal",
            takDikenal.isEmpty(),
        )
    }
}
