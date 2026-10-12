package id.or.kemenag.yasintahlil.feature.tentang

import id.or.kemenag.yasintahlil.core.legal.SumberKonten
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pengaturan Aplikasi": *"Tentang aplikasi, sumber konten, lisensi, serta kebijakan
 * privasi."* dan §"Keamanan dan Privasi": *"Dokumentasikan izin Android dan alasan penggunaannya."*
 *
 * Sebelumnya Pengaturan hanya memuat satu paragraf ringkas di dalam kode; kebijakan privasi dan
 * daftar izin yang lengkap hanya ada di `docs/`, yang tak dapat dibaca pengguna aplikasi.
 */
class TentangTest {

    private fun modul(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun repo(): File = listOf(File(".."), File(".")).first { File(it, "docs/PRIVACY.md").isFile }

    private fun sumber(relatif: String): String = File(modul(), relatif).readText()

    private val layarRel = "src/main/java/id/or/kemenag/yasintahlil/feature/tentang/TentangScreen.kt"

    /**
     * Dokumen di assets adalah salinan apa adanya dari berkas repositori. Tanpa penjagaan ini,
     * kebijakan privasi yang dibaca pengguna bisa menyimpang dari yang didokumentasikan.
     */
    @Test
    fun dokumenLegalIdentikDenganBerkasRepositori() {
        listOf(
            "PRIVACY.md" to "docs/PRIVACY.md",
            "PERMISSIONS.md" to "docs/PERMISSIONS.md",
            "LICENSE.txt" to "LICENSE",
        ).forEach { (aset, asal) ->
            val a = File(modul(), "src/main/assets/legal/$aset")
            assertTrue("aset legal hilang: $aset", a.isFile)
            assertTrue("$aset tidak identik dengan $asal",
                a.readBytes().contentEquals(File(repo(), asal).readBytes()))
        }
    }

    @Test
    fun layarMenampilkanKeempatHalYangDiminta() {
        val s = sumber(layarRel)
        listOf("Tentang aplikasi", "Sumber konten", "Kebijakan privasi",
            "Izin aplikasi dan alasannya", "Lisensi").forEach {
            assertTrue("layar Tentang tidak memuat bagian '$it'", s.contains(it))
        }
        assertTrue("versi aplikasi tidak ditampilkan", s.contains("BuildConfig.VERSION_NAME"))
    }

    @Test
    fun layarMembacaDokumenDariAsetBukanDariKode() {
        val s = sumber(layarRel)
        listOf("DokumenLegal.PRIVASI", "DokumenLegal.IZIN", "DokumenLegal.LISENSI").forEach {
            assertTrue("layar tidak membaca $it", s.contains("DokumenLegal.baca(ctx, $it)"))
        }
        assertTrue("dokumen tidak dirender lewat pengurai bersama", s.contains("MarkdownRingkas.blok("))
    }

    /** Sumber konten dibaca dari bundel, bukan dari daftar tetap yang bisa berbohong. */
    @Test
    fun sumberKontenDibacaDariBundel() {
        val s = sumber(layarRel)
        assertTrue("layar tidak membaca sumber dari bundel", s.contains("repo.sumberDipakai()"))
        assertTrue("domain resmi tidak diberi keterangan", s.contains("SumberKonten.label(host)"))
    }

    @Test
    fun ruteTentangTerdaftarDanDapatDibukaDariPengaturan() {
        val routes = sumber("src/main/java/id/or/kemenag/yasintahlil/navigation/Routes.kt")
        assertTrue("rute TENTANG tidak ada", routes.contains("const val TENTANG = "))
        val langsung = routes.substringAfter("val LANGSUNG: Set<String> = setOf(").substringBefore(")")
        assertTrue("TENTANG tidak boleh dibuka dari notifikasi", langsung.contains("TENTANG"))

        val nav = sumber("src/main/java/id/or/kemenag/yasintahlil/navigation/AppNav.kt")
        assertTrue("layar Tentang tidak terpasang di navigasi",
            nav.contains("composable(Routes.TENTANG) { TentangScreen() }"))

        val pengaturan = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak membuka layar Tentang", pengaturan.contains("go(Routes.TENTANG)"))
        // NavSmokeTest mencari teks ini; judulnya harus tetap ada.
        assertTrue("judul bagian Tentang di Pengaturan hilang",
            pengaturan.contains("Tentang & sumber"))
    }

    /** Domain resmi yang boleh dipakai konten harus punya keterangan pemiliknya. */
    @Test
    fun setiapDomainResmiPunyaKeterangan() {
        val tanpaNama = ContentValidator.allowedHosts.filter { SumberKonten.label(it) == null }
        assertTrue("domain resmi tanpa keterangan: $tanpaNama", tanpaNama.isEmpty())
    }

    @Test
    fun setiapSumberYangDipakaiBundelDikenali() {
        val dir = File(modul(), "src/main/assets/content")
        val berkas = dir.listFiles { f -> f.extension == "json" }?.toList().orEmpty()
        assertTrue("berkas konten tidak ditemukan", berkas.isNotEmpty())
        val host = berkas
            .mapNotNull { f -> runCatching { JSONObject(f.readText()).optString("sumber_url") }.getOrNull() }
            .mapNotNull { ContentValidator.hostOf(it) }
            .distinct()
        assertTrue("tidak ada sumber_url yang terbaca dari bundel", host.isNotEmpty())
        host.forEach { h ->
            assertNotNull("sumber '$h' dipakai bundel tetapi tak dikenal layar Tentang",
                SumberKonten.label(h))
        }
    }
}
