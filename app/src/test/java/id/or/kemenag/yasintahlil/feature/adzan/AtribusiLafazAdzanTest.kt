package id.or.kemenag.yasintahlil.feature.adzan

import org.junit.Assert.assertFalse
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

    // ---- Catatan perbedaan susunan iqomah: satu keterangan, satu rumah ----

    /**
     * Catatan itu tinggal di **bagiannya sendiri** (`iqomah.catatan_mazhab`), bukan di tingkat modul.
     *
     * Berkasnya dahulu memuat keterangan ini dua kali: salinan tingkat modul (yang dibaca
     * [ContentRepo.iqomahCatatan], tanpa contoh perbedaannya) dan salinan di dalam `iqomah` (tak
     * pernah dibaca, tetapi menyebut *"(mis. jumlah lafaz)"*). Yang tampil di layar Iqomah karena
     * itu bukan salinan yang setara, melainkan salinan yang **kehilangan contoh** yang menjelaskan
     * apa yang berbeda antar mazhab.
     *
     * Konvensi berkasnya sendiri sudah menunjukkannya: catatan yang berlaku untuk satu bagian
     * tinggal di dalam bagian itu — `adzan.catatan_subuh` dibaca dari `adzan`, dan `iqomah` punya
     * `judul`/`sumber`/`sumber_url` sendiri. Uji ini menjaga kedua-duanya sekaligus, supaya
     * perpindahan ke arah sebaliknya (catatan bagian naik ke tingkat modul) ikut ketahuan.
     */
    @Test
    fun catatanPerBagianTinggalDiBagiannya() {
        val d = org.json.JSONObject(sumber("src/main/assets/content/adzan.json"))
        assertTrue("iqomah.json tidak lagi mencantumkan catatan_mazhab di bagiannya",
            d.getJSONObject("iqomah").has("catatan_mazhab"))
        assertFalse("catatan_mazhab kembali disalin ke tingkat modul",
            d.has("catatan_mazhab"))
        assertTrue("adzan tidak lagi mencantumkan catatan_subuh di bagiannya",
            d.getJSONObject("adzan").has("catatan_subuh"))
        assertFalse("catatan_subuh disalin ke tingkat modul",
            d.has("catatan_subuh"))
    }

    /**
     * Yang tinggal adalah ejaan yang **lebih lengkap** — salinan tingkat modul yang dahulu dibaca
     * berbunyi *"…menurut mazhab/tradisi; perbedaan dicatat apa adanya"*, sedangkan yang di dalam
     * `iqomah` menambahkan contohnya. Memindahkan yang lebih pendek berarti layar terus menyembunyikan
     * *apa* yang berbeda antar mazhab.
     */
    @Test
    fun catatanMazhabIqomahMenyebutContohPerbedaan() {
        val isi = org.json.JSONObject(sumber("src/main/assets/content/adzan.json"))
            .getJSONObject("iqomah").getString("catatan_mazhab").lowercase()
        assertTrue("catatan mazhab iqomah tidak lagi menyebut contoh perbedaannya: $isi",
            isi.contains("mis. jumlah lafaz"))
        assertTrue("catatan mazhab iqomah tidak lagi menyatakan perbedaan dicatat apa adanya: $isi",
            isi.contains("apa adanya"))
    }

    /** Pembacaan itu harus menunjuk bagiannya, bukan tingkat modul. */
    @Test
    fun repoMembacaCatatanMazhabIqomahDariBagiannya() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt")
        val badan = s.substringAfter("fun iqomahCatatan()").substringBefore("fun iqomah(")
        assertTrue("ContentRepo tidak membaca catatan mazhab dari bagian iqomah",
            badan.contains("getJSONObject(\"iqomah\").optString(\"catatan_mazhab\")"))
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
        // Aturan "lafaz ke-n bernomor n, mulai 1" kini tinggal di `JenisBacaan.idLafazIqomah`,
        // bukan disusun di layar. Dulu layar ini menulis `id(JenisBacaan.IQOMAH, (i + 1)…)` dan
        // indeks pencarian menulis `id(JenisBacaan.IQOMAH, i…)` — lafaz yang sama tersimpan dengan
        // dua rujukan berbeda. Karena itu yang diuji di sini adalah pemakaian aturan bersamanya;
        // bahwa tak ada tempat pemakaian yang menyusunnya sendiri dijaga
        // `JenisBacaanSatuTempatTest.nomorUrutLafazIqomahTidakDihitungDiTempatPemakaian`.
        assertTrue("id kartu iqomah tidak memakai aturan nomor urut bersama",
            iqomah.contains("JenisBacaan.idLafazIqomah(i + 1)"))
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

    /**
     * Catatan perbedaan susunan iqomah tetap ditampilkan apa adanya.
     *
     * Yang diperiksa adalah **syarat** tampilnya, bukan sekadar adanya pemanggilan: cabang yang
     * dimatikan (`if (false)`) tetap memuat `repo.iqomahCatatan()` di dalam badannya, jadi uji yang
     * hanya mencari nama fungsinya akan tetap hijau meski layarnya berhenti menampilkan apa pun —
     * mutasi `F layarIqomahBerhentiMenampilkanCatatan` pada `mutate65.py` memang lolos sebelum
     * syaratnya ikut diperiksa.
     */
    @Test
    fun catatanPerbedaanSusunanTetapTampil() {
        val s = layarIqomah()
        assertTrue("layar Iqomah tidak lagi menampilkan catatan perbedaan susunan",
            s.contains("repo.iqomahCatatan()"))
        assertTrue("layar Iqomah memanggil catatannya di cabang yang tidak pernah berjalan",
            s.contains("if (repo.iqomahCatatan().isNotBlank()) {"))
    }
}
