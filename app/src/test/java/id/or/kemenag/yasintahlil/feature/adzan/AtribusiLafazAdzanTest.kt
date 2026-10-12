package id.or.kemenag.yasintahlil.feature.adzan

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Lafaz adzan dan iqomah berasal dari hadis — berkas `adzan.json` sudah mencatat rujukannya
 * (`adzan.sumber` = *"HR Bukhari-Muslim — sunnah.com"*, `iqomah.sumber` = *"HR Abu Dawud, Tirmidzi —
 * sunnah.com"*), tetapi keterangan itu **tidak pernah sampai ke layar**: `AdzanLafaz` tak punya
 * kolom sumber, jadi kartu bacaan tampil tanpa asal-usul.
 *
 * Uji ini menjaga agar rujukan itu dibaca, diteruskan, dan ditampilkan; dan agar tiap lafaz punya
 * nomor urut sendiri — sebelumnya judulnya sama ("Adzan"/"Iqomah") dan id kartunya memakai 10 huruf
 * Arab pertama, yang berulang untuk lafaz yang sama, sehingga favorit satu lafaz menandai lafaz lain.
 */
class AtribusiLafazAdzanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun layarAdzan() = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt")

    private fun layarIqomah() =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt")

    // ---- Model & pembacaan bundel ----

    /** Tanpa kolom ini, tidak ada tempat menyimpan rujukan yang sudah dibaca dari berkas. */
    @Test
    fun modelLafazPunyaKolomSumber() {
        val m = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Models.kt")
        // Dipotong sampai deklarasi berikutnya, bukan sampai tanda ")" pertama: KDoc-nya sendiri
        // memuat tanda kurung, jadi memotong pada ")" akan berhenti sebelum kolomnya terbaca.
        val badan = m.substringAfter("data class AdzanLafaz(").substringBefore("data class AdzanDoa")
        assertTrue("AdzanLafaz tidak punya kolom sumber", badan.contains("val sumber: String"))
    }

    /**
     * Rujukan diambil dari entri itu sendiri, dan bila entri tidak menyebutkannya dipakai
     * keterangan tingkat bagian. Bagian `adzan`/`iqomah` di bundel hanya mencatat `sumber` di
     * tingkat bagian, jadi tanpa cadangan ini semua lafaz akan tampil tanpa sumber.
     */
    @Test
    fun repoMembacaSumberDenganCadanganTingkatBagian() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt")
        val adzan = s.substringAfter("fun adzan(): List<AdzanLafaz>").substringBefore("fun adzanCatatan")
        val iqomah = s.substringAfter("fun iqomah(): List<AdzanLafaz>").substringBefore("fun doaAdzan")
        listOf("adzan" to adzan, "iqomah" to iqomah).forEach { (nama, badan) ->
            assertTrue("$nama() tidak membaca keterangan sumber tingkat bagian",
                badan.contains("optString(\"sumber\", \"\")"))
            assertTrue("$nama() tidak meneruskan sumber per-entri dengan cadangan bagian",
                badan.contains("optString(\"sumber\", defSumber)"))
        }
    }

    /** Cadangan itu hanya berguna bila bundelnya memang mencantumkan `sumber` di tiap bagian. */
    @Test
    fun bundelMencantumkanSumberLafaz() {
        val json = sumber("src/main/assets/content/adzan.json")
        assertTrue("adzan.json tidak mencantumkan sumber lafaz adzan",
            json.contains("HR Bukhari-Muslim"))
        assertTrue("adzan.json tidak mencantumkan sumber lafaz iqomah",
            json.contains("HR Abu Dawud, Tirmidzi"))
    }

    // ---- Layar ----

    @Test
    fun layarAdzanMenampilkanSumberLafaz() {
        val s = layarAdzan()
        assertTrue("layar Adzan tidak meneruskan sumber ke kartu lafaz", s.contains("sumber = l.sumber"))
    }

    @Test
    fun layarIqomahMenampilkanSumberLafaz() {
        val s = layarIqomah()
        assertTrue("layar Iqomah tidak meneruskan sumber ke kartu lafaz", s.contains("sumber = l.sumber"))
    }

    /** Nomor urut menjadikan id kartu unik sekaligus menyatakan urutan bacaan. */
    @Test
    fun tiapLafazDiberiNomorUrutDanIdUnik() {
        val adzan = layarAdzan()
        assertTrue("lafaz adzan tidak diberi nomor urut", adzan.contains("forEachIndexed"))
        // Jenis idnya datang dari kosakata bersama `JenisBacaan`, bukan literal yang dieja di layar.
        assertTrue("id kartu adzan tidak memakai nomor urut dari kosakata bersama",
            adzan.contains("JenisBacaan.id(JenisBacaan.ADZAN, (i + 1).toString())"))
        assertTrue("judul kartu adzan tidak menyebut urutannya", adzan.contains("dari \${lafaz.size}"))

        val iqomah = layarIqomah()
        assertTrue("lafaz iqomah tidak diberi nomor urut", iqomah.contains("forEachIndexed"))
        assertTrue("id kartu iqomah tidak memakai nomor urut dari kosakata bersama",
            iqomah.contains("JenisBacaan.id(JenisBacaan.IQOMAH, (i + 1).toString())"))
        assertTrue("judul kartu iqomah tidak menyebut urutannya", iqomah.contains("dari \${items.size}"))
    }

    /** Id yang memakai 10 huruf Arab pertama berulang untuk lafaz yang sama — sumber tabrakan. */
    @Test
    fun idKartuTidakLagiMemakaiPotonganArab() {
        listOf("AdzanScreen.kt" to layarAdzan(), "IqomahDoaScreen.kt" to layarIqomah()).forEach { (nama, isi) ->
            assertTrue("$nama masih memakai potongan Arab sebagai id kartu",
                !isi.contains("BacaanCard(\"adzan:\${l.arab.take(10)}\"") &&
                    !isi.contains("BacaanCard(\"iqomah:\${l.arab.take(10)}\""))
        }
    }

    /** Catatan perbedaan susunan iqomah tetap ditampilkan apa adanya. */
    @Test
    fun catatanPerbedaanSusunanTetapTampil() {
        val s = layarIqomah()
        assertTrue("layar Iqomah tidak lagi menampilkan catatan perbedaan susunan",
            s.contains("repo.iqomahCatatan()"))
    }
}
