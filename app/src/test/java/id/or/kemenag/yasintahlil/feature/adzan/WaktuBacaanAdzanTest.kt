package id.or.kemenag.yasintahlil.feature.adzan

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Bacaan Setelah Adzan" meminta *"Penjelasan waktu membacanya"* untuk setiap bacaan:
 * menjawab adzan, sholawat, dan doa setelah adzan.
 *
 * Urutan saja tidak cukup — pengguna yang membuka layar saat adzan sedang berkumandang perlu tahu
 * bacaan mana yang berlaku **sekarang**, bukan hanya urutannya. Karena itu tiap entri menyatakan
 * waktunya sendiri, dan layar menampilkannya lewat satu jalur render yang sama supaya tidak ada
 * entri yang keterangannya diam-diam terlewat.
 */
class WaktuBacaanAdzanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val layarRel = "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt"
    private val repoRel = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"

    /** Kunci entri di `adzan.json` beserta pembaca yang seharusnya mengambilnya dari repo. */
    private val entri = listOf(
        "menjawab_adzan" to "repo.menjawabAdzan()",
        "sholawat_setelah_adzan" to "repo.sholawatAdzan()",
        "doa_setelah_adzan" to "repo.doaAdzan()",
    )

    @Test
    fun setiapBacaanAdzanMenyatakanKapanDibaca() {
        val d = JSONObject(sumber("src/main/assets/content/adzan.json"))
        entri.forEach { (k, _) ->
            val waktu = d.getJSONObject(k).optString("waktu")
            assertTrue("'$k' tidak menyatakan kapan dibacanya", waktu.isNotBlank())
            // Waktunya harus menjelaskan, bukan sekadar mengulang judul.
            assertTrue("'$k' punya waktu yang terlalu pendek untuk menjelaskan: '$waktu'",
                waktu.length >= 25)
        }
    }

    /**
     * Setiap kunci JSON punya pembaca di repo. Tanpa ini, entri yang ditambahkan ke bundel bisa
     * tidak terbaca layar mana pun dan waktunya hilang tanpa jejak.
     */
    @Test
    fun setiapEntriDibacaRepo() {
        val repo = sumber(repoRel)
        entri.forEach { (k, pembaca) ->
            assertTrue("repo tidak membaca entri '$k' (diharapkan $pembaca)",
                repo.contains("adzanDoa(\"$k\")"))
        }
    }

    /** Layar menampilkan waktu tiap bacaan, bukan hanya urutannya. */
    @Test
    fun layarMenampilkanWaktuTiapBacaan() {
        val layar = sumber(layarRel)
        assertTrue("layar tidak menampilkan waktu bacaan", layar.contains("Kapan dibaca:"))
        assertTrue("layar tidak membaca field waktu", layar.contains("entri.waktu"))
        // Ketiga bacaan harus lewat jalur render yang sama; sebelumnya dua entri dirender langsung
        // dan catatan pada entri lain akan terlewat tanpa jejak.
        entri.forEach { (_, pembaca) ->
            assertTrue("layar tidak memuat bacaan lewat $pembaca", layar.contains(pembaca))
        }
        assertTrue("bacaan tidak dirender lewat satu jalur bersama", layar.contains("KartuDoa("))
    }

    /** Catatan yang menyertai bacaan tidak boleh hilang hanya karena entrinya berbeda. */
    @Test
    fun catatanDirenderUntukSemuaEntri() {
        val layar = sumber(layarRel)
        assertTrue("catatan tidak dirender dari jalur bersama", layar.contains("entri.catatan"))
    }
}
