package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `README.md` memuat satu baris yang menyatakan patokan toolchain — *"Toolchain dipatok (AGP 8.13.x,
 * Kotlin 2.3.21, `compileSdk`/`targetSdk` 36)"* — dan baris itu yang pertama dibaca kontributor
 * sebelum membangun. Nilainya hidup di berkas lain: versi AGP dan Kotlin di
 * `gradle/libs.versions.toml`, sedangkan `compileSdk`/`targetSdk` dan tingkat Java di
 * `app/build.gradle.kts`. Tak ada apa pun yang membandingkannya.
 *
 * Dokumen yang mematok versi menua diam-diam: setelah AGP naik, README tetap menuliskan yang lama,
 * dan kontributor yang membangun dengan toolchain berbeda mengejar galat yang tak ada di proyek.
 * Uji ini membandingkan **keduanya**, sehingga baris itu hanya dapat berbohong bila seseorang
 * mengubah nilai dan dokumennya sekaligus.
 */
class ReadmeToolchainTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    /**
     * Akar repositori, tempat `app/` dan `gradle/` bersebelahan. Uji berjalan dengan direktori kerja
     * modul (`app/`), jadi [root] menunjuk ke `app`.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "gradle/libs.versions.toml").isFile }
            ?: error("gradle/libs.versions.toml tidak ditemukan dari ${root().absolutePath}")

    private fun baca(relatif: String): String = File(repoRoot(), relatif).readText()

    /** Baris patokan toolchain di README — satu-satunya tempat nilainya dinyatakan. */
    private fun barisPatokan(): String =
        baca("README.md").lines().singleOrNull { it.contains("Toolchain dipatok") }
            ?: error("README.md tidak lagi memuat baris 'Toolchain dipatok'")

    private fun versiDiToml(nama: String): String =
        Regex("^$nama\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE)
            .find(baca("gradle/libs.versions.toml"))?.groupValues?.get(1)
            ?: error("versi '$nama' tidak ditemukan di gradle/libs.versions.toml")

    private fun angkaDiGradle(nama: String): Int =
        Regex("$nama\\s*=\\s*(\\d+)").find(baca("app/build.gradle.kts"))
            ?.groupValues?.get(1)?.toInt()
            ?: error("'$nama' tidak ditemukan di app/build.gradle.kts")

    /**
     * AGP disebut sebagai major.minor (`8.13.x`) — itulah yang menentukan kompatibilitas plugin,
     * sedangkan patch-nya bebas naik. Yang harus sama adalah major.minornya, bukan seluruh versinya.
     */
    @Test
    fun versiAgpDiReadmeSelarasDenganKatalogVersi() {
        val diReadme = Regex("""AGP (\d+\.\d+)(?:\.\d+|\.x)?""").find(barisPatokan())
            ?.groupValues?.get(1)
        assertTrue("README.md tidak lagi menyebut versi AGP di baris patokan toolchain",
            diReadme != null)

        val dariToml = versiDiToml("agp")
        assertEquals(
            "versi AGP di README.md berbeda dari gradle/libs.versions.toml ($dariToml) — " +
                "kontributor akan membangun dengan toolchain yang tidak dipakai proyek",
            dariToml.split(".").take(2).joinToString("."), diReadme,
        )
    }

    @Test
    fun versiKotlinDiReadmeSelarasDenganKatalogVersi() {
        val diReadme = Regex("""Kotlin (\d+\.\d+\.\d+)""").find(barisPatokan())
            ?.groupValues?.get(1)
        assertTrue("README.md tidak lagi menyebut versi Kotlin di baris patokan toolchain",
            diReadme != null)
        assertEquals("versi Kotlin di README.md berbeda dari gradle/libs.versions.toml",
            versiDiToml("kotlin"), diReadme)
    }

    /**
     * README menuliskan satu angka untuk `compileSdk`/`targetSdk` — jadi angkanya harus sama dengan
     * kedua nilai di Gradle **dan** keduanya harus benar-benar sama, sebab satu angka tak dapat
     * mewakili dua nilai yang berbeda.
     */
    @Test
    fun tingkatSdkDiReadmeSelarasDenganKonfigurasiGradle() {
        val diReadme = Regex("""targetSdk`?\s+(\d+)""").find(barisPatokan())
            ?.groupValues?.get(1)?.toInt()
        assertTrue("README.md tidak lagi menyebut targetSdk di baris patokan toolchain",
            diReadme != null)
        assertEquals("targetSdk di README.md berbeda dari app/build.gradle.kts",
            angkaDiGradle("targetSdk"), diReadme)
        assertEquals("compileSdk di README.md berbeda dari app/build.gradle.kts",
            angkaDiGradle("compileSdk"), diReadme)
        assertEquals(
            "compileSdk dan targetSdk kini benar-benar berbeda, tetapi README menyebut satu angka " +
                "untuk keduanya — baris itu harus ditulis ulang",
            angkaDiGradle("compileSdk"), angkaDiGradle("targetSdk"),
        )
    }

    @Test
    fun tingkatJavaDiReadmeSelarasDenganKonfigurasiGradle() {
        val diReadme = Regex("""JDK (\d+)""").find(baca("README.md"))?.groupValues?.get(1)
        assertTrue("README.md tidak lagi menyebut JDK yang dibutuhkan", diReadme != null)
        // Dua bentuk DSL yang sah: `kotlinOptions { jvmTarget = "17" }` (lama) dan
        // `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` (Kotlin 2.3+). Keduanya
        // diterima agar uji ini membandingkan nilainya, bukan menuntut satu sintaks.
        val diGradle = Regex("""jvmTarget\.set\(\s*JvmTarget\.JVM_(\d+)|jvmTarget\s*=\s*"(\d+)""")
            .find(baca("app/build.gradle.kts"))
            ?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
        assertTrue("jvmTarget tidak ditemukan di app/build.gradle.kts", diGradle != null)
        assertEquals("tingkat Java di README.md berbeda dari app/build.gradle.kts",
            diGradle, diReadme)
    }
}
