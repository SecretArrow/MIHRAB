package id.or.kemenag.yasintahlil.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Bundel konten mencatat keterangan **tingkat modul** yang berlaku untuk seluruh isinya, tetapi
 * sebagian tidak pernah dibaca kode — sehingga layar mengarang versi hardcode yang lebih pendek,
 * atau tidak menyebut asal-usulnya sama sekali:
 *
 * - `tahlil.json.catatan_tradisi` — layar Tahlil menulis *"susunan dapat berbeda menurut tradisi"*,
 *   padahal berkasnya memuat kalimat yang lebih lengkap ("…; bukan satu-satunya versi yang benar").
 * - `tahlil_full.json.sumber_umum` — sebagian besar bagian tahlil tidak menyebut sumbernya sendiri,
 *   jadi tanpa keterangan tingkat modul ini kartunya tampil tanpa asal-usul.
 * - `sholat_daftar.json.panduan` — layar menulis "(Fasolatan Kemenag)", berkasnya mencatat yang
 *   lebih tepat termasuk bahwa hadisnya diverifikasi lewat sunnah.com.
 *
 * Uji ini menjaga agar ketiganya dibaca dari berkas dan benar-benar tampil.
 */
class AtribusiModulTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val repoRel = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"

    // ---- Pembacaan bundel ----

    @Test
    fun repoMembacaKeteranganTingkatModul() {
        val s = sumber(repoRel)
        // Nama berkasnya datang dari `BerkasKonten` (lihat `BerkasKontenSatuTempatTest`), jadi yang
        // diperiksa di sini adalah pasangan **berkas + kunci**-nya, bukan ejaan nama berkasnya.
        assertTrue("ContentRepo tidak membaca catatan_tradisi dari tahlil.json",
            s.contains("doc(BerkasKonten.TAHLIL_SUSUNAN).optString(\"catatan_tradisi\""))
        assertTrue("ContentRepo tidak membaca sumber_umum dari tahlil_full.json",
            s.contains("doc(BerkasKonten.TAHLIL_FULL).optString(\"sumber_umum\""))
        assertTrue("ContentRepo tidak membaca panduan dari sholat_daftar.json",
            s.contains("doc(BerkasKonten.SHOLAT_DAFTAR).optString(\"panduan\""))
    }

    /** Pembacaan itu hanya berguna bila berkasnya memang mencantumkan kuncinya. */
    @Test
    fun bundelMencantumkanKeteranganTingkatModul() {
        assertTrue("tahlil.json tidak mencantumkan catatan_tradisi",
            sumber("src/main/assets/content/tahlil.json").contains("\"catatan_tradisi\""))
        assertTrue("tahlil_full.json tidak mencantumkan sumber_umum",
            sumber("src/main/assets/content/tahlil_full.json").contains("\"sumber_umum\""))
        assertTrue("sholat_daftar.json tidak mencantumkan panduan",
            sumber("src/main/assets/content/sholat_daftar.json").contains("\"panduan\""))
    }

    // ---- Layar ----

    @Test
    fun layarTahlilMenampilkanSumberBacaanTingkatModul() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilScreen.kt")
        assertTrue("layar Tahlil tidak memakai repo.tahlilSumberBacaan()",
            layar.contains("repo.tahlilSumberBacaan()"))
        assertTrue("layar Tahlil tidak menampilkan sumber bacaan tingkat modul",
            layar.contains("Sumber bacaan: "))
    }

    @Test
    fun layarTahlilMembacaCatatanTradisiDariBundel() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilScreen.kt")
        assertTrue("layar Tahlil tidak membaca catatan tradisi dari bundel",
            layar.contains("repo.tahlilCatatanTradisi()"))
        // Kalimat cadangan tetap ada supaya layar tidak kosong bila berkasnya kehilangan kuncinya.
        assertTrue("layar Tahlil kehilangan kalimat cadangan catatan tradisi",
            layar.contains("Susunan dapat berbeda menurut tradisi."))
    }

    @Test
    fun layarSholatMenampilkanSumberPanduan() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatScreen.kt")
        assertTrue("layar Sholat tidak memakai repo.sholatPanduan()",
            layar.contains("repo.sholatPanduan()"))
        assertTrue("layar Sholat tidak menampilkan sumber panduan",
            layar.contains("Sumber panduan: "))
        assertFalse("layar Sholat masih mengarang sumber di judulnya",
            layar.contains("Panduan Sholat (Fasolatan Kemenag)"))
    }
}
