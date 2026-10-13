package id.or.kemenag.yasintahlil.feature.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.legal.SumberKonten
import id.or.kemenag.yasintahlil.core.sholat.TampilanSholatKhusus
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rujukan tambahan sholat ghaib pernah tinggal di **dua** tempat dengan isi yang berbeda:
 * kalimatnya diketik di layar (`GhaibScreen.CATATAN_DALIL` — *"Rujukan tata cara: imbauan Kemenag;
 * syarat & kritik sanad: NU Online"*), sedangkan `sholat_ghaib.json#sumber_tambahan` hanya
 * mencantumkan `kemenag.go.id` dan `sunnah.com/muslim:394`. **NU Online tak pernah tercatat di
 * bundel**, padahal layar menyatakannya sebagai rujukan syarat & kritik sanad.
 *
 * Akibatnya bukan sekadar salinan yang berbeda: layar Tentang membangun daftar sumbernya dari
 * seluruh nilai bersumber pada bundel dan menjanjikan daftar itu memuat **setiap** domain yang
 * dirujuk ("tidak dapat bertambah diam-diam"). Karena `nu.or.id` tak ada di bundel, janji itu
 * bocor ke arah sebaliknya — domain yang benar-benar disebut layar justru **hilang** dari daftar.
 *
 * Uji ini menjaga agar kalimat rujukan tinggal di bundel (satu rumah), agar setiap domain yang
 * disebutnya benar-benar tercatat di `sumber_tambahan` sehingga layar Tentang dapat menyebutnya,
 * dan agar layar Ghaib membacanya — bukan mengetiknya lagi.
 */
class AtribusiRujukanGhaibTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun teks(relatif: String): String = File(root(), relatif).readText()

    private fun ghaib(): JSONObject = JSONObject(teks("src/main/assets/content/sholat_ghaib.json"))

    private fun jenazah(): JSONObject = JSONObject(teks("src/main/assets/content/sholat_jenazah.json"))

    private fun layar(nama: String): String =
        teks("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/$nama")

    private fun repo(): String =
        teks("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt")

    // ---- Bundel: kalimat rujukan dan daftar domainnya ----

    /** Tanpa medan ini tak ada tempat menyimpan kalimat rujukan yang selama ini diketik di layar. */
    @Test
    fun kalimatRujukanTinggalDiBundelGhaib() {
        assertTrue("sholat_ghaib.json tidak lagi memuat catatan_rujukan",
            ghaib().has("catatan_rujukan"))
        val isi = ghaib().getString("catatan_rujukan")
        assertTrue("catatan rujukan ghaib kosong", isi.isNotBlank())
        assertTrue("catatan rujukan ghaib tidak lagi menyebut imbauan Kemenag: $isi",
            isi.contains("Kemenag"))
        assertTrue("catatan rujukan ghaib tidak lagi menyebut NU Online: $isi",
            isi.contains("NU Online"))
    }

    /**
     * Setiap domain yang disebut kalimat rujukan harus benar-benar tercatat di `sumber_tambahan`.
     *
     * Inilah inti kekeliruannya dahulu: kalimatnya menyebut NU Online sementara daftar domainnya
     * tidak, sehingga layar Tentang — yang membaca daftar itu, bukan kalimat layar — tak pernah
     * menyebut `nu.or.id`.
     */
    @Test
    fun domainYangDisebutRujukanTercatatDiSumberTambahan() {
        // Dibandingkan pada domain induknya: `sumber_tambahan` memakai `www.kemenag.go.id`,
        // sedangkan layar Tentang menyebut induknya `kemenag.go.id`.
        val induk = ContentValidator.sumberUrl(ghaib())
            .mapNotNull { ContentValidator.hostOf(it) }
            .mapNotNull { SumberKonten.induk(it) }
            .toSet()
        listOf("kemenag.go.id", "sunnah.com", "nu.or.id").forEach { h ->
            assertTrue("sumber_tambahan sholat_ghaib.json tidak memuat domain $h (dapat: $induk)",
                induk.contains(h))
        }
    }

    /** Layar Tentang hanya dapat menyebut domain yang punya keterangan pemiliknya. */
    @Test
    fun nuOnlineDikenaliLayarTentang() {
        assertNotNull("nu.or.id tak dikenal layar Tentang, padahal dirujuk layar Ghaib",
            SumberKonten.label("nu.or.id"))
    }

    /**
     * Rujukan kritik sanad itu **khas Ghaib**: syarat sholat jenazah tidak bersandar padanya.
     * Salinan yang menyeberang akan mengklaim sumber untuk modul yang tidak memakainya, jadi
     * perbedaannya dipaku — menambahkannya kelak harus keputusan sadar, bukan salin-tempel.
     */
    @Test
    fun jenazahTidakMenyalinCatatanRujukanGhaib() {
        assertFalse("sholat_jenazah.json menyalin catatan rujukan khas Ghaib",
            jenazah().has("catatan_rujukan"))
    }

    // ---- Pembacaan & tampilan ----

    @Test
    fun repoMembacaCatatanRujukanDariBundel() {
        val badan = repo().substringAfter("fun sholatKhusus(").substringBefore("fun sumberDipakai(")
        assertTrue("ContentRepo tidak membaca catatan_rujukan dari bundel",
            badan.contains("optString(\"catatan_rujukan\")"))
    }

    /** Layar membacanya, bukan mengetiknya lagi — kalimat dan daftar domain kini tak dapat berbeda. */
    @Test
    fun layarGhaibMenampilkanRujukanDariBundel() {
        val s = layar("GhaibScreen.kt")
        assertTrue("layar Ghaib tidak meneruskan catatan rujukan bundel ke baris dalil",
            s.contains("TampilanSholatKhusus.dalil(info.dalil, info.catatanRujukan)"))
        // Diperiksa pada kode tanpa komentar: KDoc layar ini memang bercerita tentang `CATATAN_DALIL`
        // yang dahulu diketik di sini, dan cerita itu bukan salinan yang dilarang.
        val kode = KodeSumber.kodeTanpaKomentar(s)
        assertTrue("layar Ghaib masih mengetik kalimat rujukannya sendiri",
            !kode.contains("CATATAN_DALIL"))
        assertTrue("layar Ghaib masih memuat salinan kalimat rujukan",
            !kode.contains("Rujukan tata cara:"))
    }

    /**
     * Aturan "dalil disambung rujukan modulnya" tinggal di satu tempat, jadi berkas Jenazah yang
     * kelak menambah `catatan_rujukan` tak perlu mengubah layarnya.
     */
    @Test
    fun keduaLayarMemakaiAturanSambunganYangSama() {
        assertTrue("layar Jenazah tidak memakai aturan sambungan dalil-rujukan bersama",
            layar("JenazahScreen.kt").contains("TampilanSholatKhusus.dalil(info.dalil, info.catatanRujukan)"))
    }

    /** Perangkaiannya dipaku: rujukan hanya disambung bila ada, dan tidak meninggalkan spasi ganda. */
    @Test
    fun perangkaianDalilDanRujukanSatuAturan() {
        assertEquals("Dalil: HR contoh.", TampilanSholatKhusus.dalil("HR contoh."))
        assertEquals("Dalil: HR contoh.", TampilanSholatKhusus.dalil("HR contoh.", ""))
        assertEquals("Dalil: HR contoh. Rujukan: a.",
            TampilanSholatKhusus.dalil("HR contoh.", "Rujukan: a."))
    }
}
