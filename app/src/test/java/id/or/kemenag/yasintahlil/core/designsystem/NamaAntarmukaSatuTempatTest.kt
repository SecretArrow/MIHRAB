package id.or.kemenag.yasintahlil.core.designsystem

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nama **kendali antarmuka** yang muncul di lebih dari satu tempat hanya boleh hidup di
 * `NamaAntarmuka`.
 *
 * Tiga nama sudah terlanjur ditulis berulang sebelum berkas itu ada:
 *
 *  - *"Pengaturan bacaan"* adalah judul sheet `BacaanSettingsSheet` **dan** label tombol yang
 *    membukanya di sepuluh layar. Sebelas tulisan untuk satu nama: mengganti nama sheet-nya
 *    meninggalkan sepuluh tombol yang menyebut nama lama, sehingga tombol tidak lagi menyebut apa
 *    yang dibukanya — pengguna menekan "Pengaturan bacaan" lalu mendapat sheet berjudul lain.
 *  - *"Layar penuh"* adalah label chip di enam layar bacaan. Satu varian memang berbeda
 *    (`LAYAR_PENUH_IMAM` — pada Sholat Ghaib layar penuh sekaligus mode imam), dan perbedaan itu
 *    **disengaja**: yang dijanjikan tombolnya memang berbeda. Yang tidak boleh adalah salinan yang
 *    berbeda tanpa alasan.
 *  - *"Coba lagi"* ditulis dua kali **di dalam berkas yang sama**: sebagai bawaan `labelTombol`
 *    pada `EmptyState` dan sebagai literal tombol pada `ErrorState`.
 *
 * Uji ini menjaga agar ketiganya tetap satu tempat, dan agar varian yang disengaja tetap terlihat
 * sebagai varian — bukan menyamar sebagai salinan.
 */
class NamaAntarmukaSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Kode tanpa komentar: KDoc yang menjelaskan pola lama memuat tulisan yang sama persis. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun layar(relatif: String): String =
        kode(File(root(), "src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private fun uiKit(): File =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt")

    // ---- Ejaan label ---------------------------------------------------------------------------

    /**
     * Keempat nama itu **teks yang dibaca pengguna**. Mengubahnya adalah keputusan produk, bukan
     * bagian dari merapikan kode: uji ini memaksanya disengaja, sehingga penggantian nama yang
     * tidak diniatkan langsung ketahuan.
     */
    @Test
    fun ejaanLabelAntarmukaTidakBerubah() {
        assertEquals("Pengaturan bacaan", NamaAntarmuka.PENGATURAN_BACAAN)
        assertEquals("Layar penuh", NamaAntarmuka.LAYAR_PENUH)
        assertEquals("Layar penuh (imam)", NamaAntarmuka.LAYAR_PENUH_IMAM)
        assertEquals("Coba lagi", NamaAntarmuka.COBA_LAGI)
    }

    /** Nama kosong membuat tombol tanpa label — kegagalan yang tidak terlihat di layar. */
    @Test
    fun tidakAdaLayarTanpaNama() {
        val nama = mapOf(
            "PENGATURAN_BACAAN" to NamaAntarmuka.PENGATURAN_BACAAN,
            "LAYAR_PENUH" to NamaAntarmuka.LAYAR_PENUH,
            "LAYAR_PENUH_IMAM" to NamaAntarmuka.LAYAR_PENUH_IMAM,
            "COBA_LAGI" to NamaAntarmuka.COBA_LAGI,
        )
        nama.forEach { (kunci, nilai) ->
            assertTrue("nama $kunci kosong", nilai.isNotBlank())
            assertEquals("nama $kunci tidak sama dengan dirinya sendiri setelah dirapikan",
                nilai, nilai.trim())
        }
        assertEquals("empat nama berbeda tidak boleh ada yang sama", 4, nama.values.toSet().size)
    }

    /**
     * Inti perbaikan: tidak ada berkas lain yang mengeja ulang nama itu sebagai literal. Yang
     * menyalinnya ke layar dapat menyebut hal yang sama dengan kata yang berbeda — persis yang
     * terjadi sebelum `NamaAntarmuka` ada.
     */
    @Test
    fun labelTidakDisalinKeLuarNamaAntarmuka() {
        val nama = listOf(
            NamaAntarmuka.PENGATURAN_BACAAN,
            NamaAntarmuka.LAYAR_PENUH,
            NamaAntarmuka.LAYAR_PENUH_IMAM,
            NamaAntarmuka.COBA_LAGI,
        )
        val penyalin = berkasMain()
            .filter { it.name != "NamaAntarmuka.kt" }
            .filter { f -> nama.any { kode(f).contains("\"$it\"") } }
            .map { it.name }
        assertTrue("nama kendali disalin ke luar `NamaAntarmuka`: $penyalin", penyalin.isEmpty())
    }

    /**
     * Penjaga di atas hanya berarti bila pemindai komentarnya benar-benar bekerja: satu-satunya
     * sisa tulisan *"Coba lagi"* di `src/main` ada di dalam komentar `SearchScreen.kt`. Kalau
     * komentar tidak dibuang, penjaga itu akan menuduh berkas yang justru menjelaskan perbaikannya —
     * dan kalau pemindainya rusak hingga tak menemukan apa pun, penjaganya hijau tanpa arti.
     */
    @Test
    fun pemindaiKomentarBenarBenarMembuangPenjelasan() {
        val search = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/search/SearchScreen.kt",
        )
        assertTrue("prasyarat: SearchScreen.kt tidak lagi menjelaskan tombol \"Coba lagi\"",
            search.readText().contains(NamaAntarmuka.COBA_LAGI))
        assertFalse("pemindai tidak membuang komentar sehingga menuduh penjelasan yang benar",
            kode(search).contains(NamaAntarmuka.COBA_LAGI))
    }

    // ---- Judul sheet = label tombol ------------------------------------------------------------

    /**
     * Judul sheet pengaturan bacaan dan label tombol yang membukanya adalah **satu** nama, bukan dua
     * yang kebetulan sama. Karena itu `NamaAntarmuka` hanya boleh memuat satu nama untuknya: dua
     * konstanta berbeda akan dapat menyimpang, dan tombol kembali menyebut hal yang tidak dibukanya.
     */
    @Test
    fun judulSheetDanLabelTombolAdalahSatuNama() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/NamaAntarmuka.kt",
        )
        val deklarasi = Regex("=\\s*\"${Regex.escape(NamaAntarmuka.PENGATURAN_BACAAN)}\"")
            .findAll(KodeSumber.kodeTanpaKomentar(berkas)).count()
        assertEquals("nama pengaturan bacaan dideklarasikan lebih dari sekali", 1, deklarasi)

        assertTrue("judul sheet pengaturan bacaan bukan dari kosakata bersama",
            kode(uiKit()).contains("Text(NamaAntarmuka.PENGATURAN_BACAAN"))
    }

    /** Tombol yang membuka sheet harus menyebut namanya dari kosakata yang sama. */
    @Test
    fun tombolPembukaSheetMemakaiNamaYangSama() {
        val pembuka = berkasMain().filter { kode(it).contains("BacaanSettingsSheet(") }
        assertTrue("tak ada layar yang membuka sheet pengaturan bacaan", pembuka.isNotEmpty())
        val pelanggar = pembuka
            .filterNot { kode(it).contains("NamaAntarmuka.PENGATURAN_BACAAN") }
            .map { it.name }
        assertTrue("layar membuka sheet pengaturan bacaan tanpa menyebut namanya: $pelanggar",
            pelanggar.isEmpty())
    }

    // ---- Chip "Layar penuh" --------------------------------------------------------------------

    /**
     * Tiap layar yang punya chip layar penuh memakai salah satu dari dua nama bersama — dan yang
     * benar untuk layar itu. Nama yang tertukar di sini bukan sekadar beda kata: pada Sholat Ghaib
     * chip itu sekaligus menyalakan panduan mode imam, sehingga chip "Layar penuh" biasa di sana
     * menjanjikan hal yang tidak terjadi.
     */
    @Test
    fun tiapLayarChipMemakaiNamaLayarPenuhYangBenar() {
        val biasa = listOf(
            "feature/yasin/YasinScreen.kt",
            "feature/tahlil/TahlilScreen.kt",
            "feature/dzikir/DzikirScreen.kt",
            "feature/adzan/IqomahDoaScreen.kt",
            "feature/kiblat/KiblatScreen.kt",
            "feature/sholat/JenazahScreen.kt",
        )
        biasa.forEach { berkas ->
            assertTrue("$berkas tidak memakai nama chip layar penuh bersama",
                layar(berkas).contains("Text(NamaAntarmuka.LAYAR_PENUH)"))
        }
        val ghaib = layar("feature/sholat/GhaibScreen.kt")
        assertTrue("Sholat Ghaib tidak memakai varian chip mode imam",
            ghaib.contains("Text(NamaAntarmuka.LAYAR_PENUH_IMAM)"))
        assertFalse("Sholat Ghaib memakai chip layar penuh biasa padahal ada varian imam",
            ghaib.contains("Text(NamaAntarmuka.LAYAR_PENUH)"))
    }

    /**
     * Varian imam harus tetap terbaca sebagai **varian** dari nama dasarnya, bukan nama lepas: yang
     * membedakan hanya penanda mode, dan itu yang dijanjikan tombolnya.
     */
    @Test
    fun varianImamTetapVarianDariNamaDasarnya() {
        assertTrue("varian imam tidak berangkat dari nama layar penuh",
            NamaAntarmuka.LAYAR_PENUH_IMAM.startsWith(NamaAntarmuka.LAYAR_PENUH))
        assertTrue("varian imam tidak berbeda dari nama dasarnya",
            NamaAntarmuka.LAYAR_PENUH_IMAM != NamaAntarmuka.LAYAR_PENUH)
        assertTrue("varian imam tidak menandai mode imam",
            NamaAntarmuka.LAYAR_PENUH_IMAM.contains("imam"))
    }

    // ---- "Coba lagi" dua tempat dalam satu berkas ----------------------------------------------

    /**
     * Dahulu `UiKit.kt` memuat nama itu dua kali di dalam dirinya sendiri: sebagai bawaan
     * `labelTombol` pada `EmptyState` dan sebagai literal tombol pada `ErrorState`. Dua tombol yang
     * sama-sama berarti "ulangi" dapat berubah kata tanpa disadari. Kini keduanya satu nama — dan
     * berkasnya harus tetap memuat **kedua** pemakaian itu, agar perbaikannya tidak menghapus salah
     * satunya.
     */
    @Test
    fun keduaTombolCobaLagiDiUiKitMemakaiNamaBersama() {
        val k = kode(uiKit())
        val pemakaian = Regex("""NamaAntarmuka\.COBA_LAGI""").findAll(k).count()
        assertTrue("kedua tombol \"Coba lagi\" di UiKit tidak memakai nama bersama (ditemukan $pemakaian)",
            pemakaian >= 2)
        assertTrue("bawaan `labelTombol` EmptyState bukan dari kosakata bersama",
            k.contains("labelTombol: String = NamaAntarmuka.COBA_LAGI"))
        assertTrue("tombol `ErrorState` bukan dari kosakata bersama",
            k.contains("Button(onClick = onRetry) { Text(NamaAntarmuka.COBA_LAGI) }"))
    }

    /** Aturan nama bersama tetap murni: tanpa Android, agar dapat diuji langsung di JVM. */
    @Test
    fun namaAntarmukaTidakMenyentuhAndroid() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/NamaAntarmuka.kt",
        )
        assertFalse("NamaAntarmuka mengimpor Android sehingga tak dapat diuji di JVM",
            berkas.contains("import android."))
    }
}
