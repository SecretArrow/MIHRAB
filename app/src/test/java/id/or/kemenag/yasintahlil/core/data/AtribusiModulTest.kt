package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
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
 * - `sholat_daftar.json.catatan_mazhab` — layar rincian langkah sholat menulis *"Indonesia umumnya
 *   Syafi'i (qunut Subuh sunnah)"*, padahal berkasnya justru mencatat qunut Subuh sebagai ketentuan
 *   yang "memiliki perbedaan pendapat": salinan itu menyatakan sebagai mapan hal yang sumbernya
 *   sendiri catat sebagai diperselisihkan.
 * - `wudhu.json.edisi` — layar Wudhu menulis parafrase yang lebih pendek daripada berkasnya,
 *   menghilangkan tayamum dan "Ngaji Fasolatan".
 *
 * Uji ini menjaga agar kelimanya dibaca dari berkas dan benar-benar tampil.
 *
 * Dua yang terakhir sengaja ditekankan: `SOURCES.md` §6 mencatat `edisi` dan `catatan_mazhab`
 * sebagai satu-satunya metadata provenans yang **tidak** diperiksa gerbang konten, sehingga
 * kelengkapannya hanya terjaga bila kode membacanya — dan ternyata tidak.
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

    // ---- Provenans: `edisi` & `catatan_mazhab` ---------------------------------------------------

    /**
     * `SOURCES.md` §6 mencatat `edisi` dan `catatan_mazhab` sebagai **dua metadata provenans yang
     * tidak diperiksa mesin**: gerbang konten tak pernah menggagalkannya, jadi kelengkapannya hanya
     * terjaga bila kode benar-benar membacanya. Kenyataannya tidak — `sholat_daftar.json`
     * `catatan_mazhab` dan `wudhu.json` `edisi` ditulis, divalidasi manusia, lalu tak pernah sampai
     * ke layar, dan layar mencetak parafrase yang menyimpang sebagai gantinya.
     */
    @Test
    fun repoMembacaProvenansTingkatModul() {
        val s = sumber(repoRel)
        assertTrue("ContentRepo tidak membaca catatan_mazhab dari sholat_daftar.json",
            s.contains("doc(BerkasKonten.SHOLAT_DAFTAR).optString(\"catatan_mazhab\""))
        assertTrue("ContentRepo tidak meneruskan edisi dari wudhu.json ke PanduanBersuci",
            s.contains("edisi = d.optString(\"edisi\")"))
    }

    /** Pembacaan itu hanya berguna bila berkasnya memang mencantumkan kuncinya. */
    @Test
    fun bundelMencantumkanProvenansTingkatModul() {
        assertTrue("sholat_daftar.json tidak mencantumkan catatan_mazhab",
            sumber("src/main/assets/content/sholat_daftar.json").contains("\"catatan_mazhab\""))
        assertTrue("wudhu.json tidak mencantumkan edisi",
            sumber("src/main/assets/content/wudhu.json").contains("\"edisi\""))
    }

    /**
     * Layar rincian langkah sholat menampilkan catatan mazhab **dari bundel**.
     *
     * Salinan lamanya menyatakan *"qunut Subuh sunnah"* — pendirian yang tidak diambil
     * `sholat_daftar.json`, yang mencatat qunut Subuh sebagai ketentuan yang "memiliki perbedaan
     * pendapat". Menampilkan berkasnya apa adanya membuat layar tidak pernah menyatakan lebih
     * daripada yang dicatat sumbernya. Komentar dibuang lebih dahulu: KDoc layar ini menyebut
     * kalimat lama itu justru untuk menjelaskan mengapa ia dibuang.
     */
    @Test
    fun layarRincianSholatMenampilkanCatatanMazhabDariBundel() {
        val kode = KodeSumber.kodeTanpaKomentar(
            sumber("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatDetailScreen.kt"))
        assertTrue("layar rincian sholat tidak membaca catatan mazhab dari bundel",
            kode.contains("repo.sholatCatatanMazhab()"))
        assertTrue("layar rincian sholat tidak menampilkan catatan mazhab dari bundel",
            kode.contains("Catatan mazhab: \" + catatanMazhab"))
        assertFalse("layar rincian sholat masih mengarang pendirian mazhab yang tidak diambil " +
            "bundelnya", kode.contains("qunut Subuh sunnah"))
    }

    /**
     * Layar Wudhu menampilkan provenans **dari bundel**.
     *
     * Salinan lamanya lebih pendek daripada `wudhu.json#edisi`: menghilangkan tayamum dan "Ngaji
     * Fasolatan", sehingga layar mengaku bersumber dari kerangka yang tidak sama dengan yang
     * dicatat kontennya.
     */
    @Test
    fun layarWudhuMenampilkanEdisiDariBundel() {
        val kode = KodeSumber.kodeTanpaKomentar(
            sumber("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/WudhuScreen.kt"))
        assertTrue("layar Wudhu tidak menampilkan edisi dari bundel", kode.contains("panduan.edisi"))
        assertFalse("layar Wudhu masih mengarang provenans versinya sendiri",
            kode.contains("Kerangka tata cara mengikuti program Fasolatan Ditjen Bimas Islam Kemenag"))
    }
}
