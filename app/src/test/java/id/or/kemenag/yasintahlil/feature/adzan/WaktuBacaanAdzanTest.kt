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

    private val entri = listOf("menjawab_adzan", "sholawat_setelah_adzan", "doa_setelah_adzan")

    @Test
    fun setiapBacaanAdzanMenyatakanKapanDibaca() {
        val d = JSONObject(sumber("src/main/assets/content/adzan.json"))
        entri.forEach { k ->
            val waktu = d.getJSONObject(k).optString("waktu")
            assertTrue("'$k' tidak menyatakan kapan dibacanya", waktu.isNotBlank())
            // Waktunya harus menjelaskan, bukan sekadar mengulang judul.
            assertTrue("'$k' punya waktu yang terlalu pendek untuk menjelaskan: '$waktu'",
                waktu.length >= 25)
        }
    }

    /** Layar menampilkan waktu tiap bacaan, bukan hanya urutannya. */
    @Test
    fun layarMenampilkanWaktuTiapBacaan() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt")
        assertTrue("layar tidak menampilkan waktu bacaan", layar.contains("Kapan dibaca:"))
        assertTrue("layar tidak membaca field waktu", layar.contains("entri.waktu"))
        // Ketiga bacaan harus lewat jalur render yang sama; sebelumnya dua entri dirender langsung
        // dan catatan pada entri lain akan terlewat tanpa jejak.
        entri.forEach { k ->
            assertTrue("layar tidak memuat '$k' lewat jalur render bersama",
                layar.contains("KartuDoa(") && layar.contains("\"$k\""))
        }
    }

    /** Catatan yang menyertai bacaan tidak boleh hilang hanya karena entrinya berbeda. */
    @Test
    fun catatanDirenderUntukSemuaEntri() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt")
        assertTrue("catatan tidak dirender dari jalur bersama", layar.contains("entri.catatan"))
    }
}
