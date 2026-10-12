package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plafon versi Dependabot (`.github/dependabot.yml`) harus konsisten dengan toolchain yang
 * benar-benar dipakai (`gradle/libs.versions.toml`).
 *
 * Alasannya konkret: plafon Hilt pernah dipasang di `>= 2.60`, padahal **2.59** sudah jadi rilis
 * pertama yang mewajibkan AGP >= 9.0.0 (catatan rilis 2.58 menyebut dukungan AGP 9 sengaja
 * ditahan). Akibatnya Dependabot mengusulkan Hilt 2.60.1 dan CI merah dengan "only compatible
 * with Android Gradle plugin (AGP) version 9.0.0 or higher". Uji ini mengunci pelajaran itu:
 * selama proyek masih di AGP 8.x, plafon Dagger/Hilt tidak boleh di atas 2.59, dan tidak ada
 * plafon yang boleh berada di bawah versi yang sedang dipakai (plafon basi).
 */
class DependabotConfigTest {

    /** Rilis pertama Dagger/Hilt yang menuntut AGP >= 9.0.0. */
    private val hiltAgp9Pertama = "2.59"

    /** Pola `dependency-name` → kunci versi di `[versions]` libs.versions.toml. */
    private val kunciVersi = mapOf(
        "com.google.dagger*" to "hilt",
        "androidx.hilt:*" to "hiltExt",
        "androidx.core:core-ktx" to "coreKtx",
        "androidx.navigation:navigation-compose" to "navigation",
        "androidx.room:*" to "room",
        "androidx.work:*" to "workmanager",
        "androidx.media3:*" to "media3",
        "androidx.test*" to "androidx-test-core",
        "androidx.test.espresso:*" to "espresso",
        "androidx.test.uiautomator:*" to "uiautomator",
    )

    private fun berkasRepo(path: String): File {
        val kandidat = listOf(File("."), File("app"), File("..")).map { File(it, path) }
        return kandidat.firstOrNull { it.isFile }
            ?: error("$path tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun toml(): String = berkasRepo("gradle/libs.versions.toml").readText()
    private fun dependabot(): String = berkasRepo(".github/dependabot.yml").readText()

    /** Nilai `kunci = "x.y.z"` dari blok `[versions]`. */
    private fun versi(kunci: String): String =
        Regex("^\\s*$kunci\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE).find(toml())
            ?.groupValues?.get(1)
            ?: error("Versi '$kunci' tidak ada di libs.versions.toml")

    /** Setiap entri `ignore` pada blok gradle: pola nama → daftar batas `>= x.y.z`. */
    private fun plafonGradle(): Map<String, List<String>> {
        val blokGradle = dependabot().substringBefore("package-ecosystem: github-actions")
        val entri = Regex(
            "-\\s*dependency-name:\\s*\"([^\"]+)\"[^\\n]*\\n\\s*versions:\\s*\\[([^\\]]*)]",
        ).findAll(blokGradle).associate { m ->
            val pola = m.groupValues[1]
            val batas = Regex("\">=\\s*([^\"]+)\"").findAll(m.groupValues[2])
                .map { it.groupValues[1] }.toList()
            pola to batas
        }
        assertTrue("Tidak ada entri ignore terbaca dari .github/dependabot.yml", entri.isNotEmpty())
        return entri
    }

    private fun bandingkan(a: String, b: String): Int {
        val pa = a.split(".", "-").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".", "-").map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(pa.size, pb.size)) { i ->
            val d = (pa.getOrElse(i) { 0 }) - (pb.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /** Setiap `dependency-name` harus punya tepat satu batas `>= x.y.z` yang bisa dibaca. */
    @Test
    fun setiap_entri_ignore_punya_batas_versi() {
        val jumlahEntri = Regex("-\\s*dependency-name:").findAll(dependabot()).count()
        val plafon = plafonGradle()
        assertEquals(
            "Ada entri ignore yang tidak memakai pola 'dependency-name' + 'versions' yang dikenali",
            jumlahEntri,
            plafon.size,
        )
        plafon.forEach { (pola, batas) ->
            assertEquals("Entri '$pola' harus punya tepat satu batas '>= x.y.z'", 1, batas.size)
            assertTrue("Batas '$pola' kosong", batas[0].isNotBlank())
        }
    }

    /** Inti pelajaran PR #22: selama AGP 8.x, Hilt 2.59+ wajib diabaikan. */
    @Test
    fun plafon_hilt_sejalan_dengan_versi_agp() {
        val agp = versi("agp")
        val batas = plafonGradle()["com.google.dagger*"]
        assertNotNull("Entri ignore untuk com.google.dagger* hilang", batas)
        val plafon = batas!!.single()
        val agp9 = (agp.split(".").firstOrNull()?.toIntOrNull() ?: 0) >= 9
        if (agp9) {
            assertTrue(
                "AGP sudah 9.x ($agp): plafon Hilt $plafon seharusnya dilepas atau dinaikkan",
                bandingkan(plafon, hiltAgp9Pertama) > 0,
            )
        } else {
            assertTrue(
                "AGP $agp (< 9) tapi plafon Dagger/Hilt dipasang di $plafon — Hilt $hiltAgp9Pertama " +
                    "sudah menolak AGP < 9.0.0, jadi plafonnya harus <= $hiltAgp9Pertama",
                bandingkan(plafon, hiltAgp9Pertama) <= 0,
            )
        }
    }

    /** Plafon basi: mengabaikan versi yang sedang dipakai berarti daftar abaikan tak lagi akurat. */
    @Test
    fun tidak_ada_plafon_di_bawah_versi_terpakai() {
        plafonGradle().forEach { (pola, batas) ->
            val kunci = kunciVersi[pola] ?: return@forEach
            val dipakai = versi(kunci)
            val plafon = batas.single()
            assertTrue(
                "Plafon '$pola' ($plafon) tidak di atas versi terpakai $kunci=$dipakai — " +
                    "plafon basi, perbarui .github/dependabot.yml",
                bandingkan(plafon, dipakai) > 0,
            )
        }
    }

    /** Setiap pola `dependency-name` harus punya pasangan kunci versi agar uji di atas bermakna. */
    @Test
    fun setiap_pola_punya_pasangan_kunci_versi() {
        val takDikenal = plafonGradle().keys - kunciVersi.keys
        assertTrue(
            "Pola dependabot tanpa pasangan kunci versi (tambahkan ke kunciVersi): $takDikenal",
            takDikenal.isEmpty(),
        )
    }
}
