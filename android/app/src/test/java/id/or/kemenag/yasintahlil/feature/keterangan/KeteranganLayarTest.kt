package id.or.kemenag.yasintahlil.feature.keterangan

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keterangan panjang kini tinggal di **satu layar** ([KeteranganScreen]), dibuka lewat ikon Info
 * kecil pada tiap layar konten — bukan lagi menumpuk di badan layar dan merusak susunannya.
 *
 * Uji ini menjaga empat hal yang mudah hilang diam-diam:
 *
 * - **Pintunya ada di mana-mana ia dijanjikan.** Tiap layar konten yang keterangannya dipindahkan
 *   harus memasang `TombolInfo` yang benar-benar menuju `Routes.keterangan(...)`; ikon yang tak
 *   membuka apa pun adalah tombol dekoratif — persis yang dilarang `UxStateTest`.
 * - **Isinya dibaca ulang dari sumber yang sama.** Layar keterangan menerima hanya `asal`, lalu
 *   membaca `ContentRepo` dan aturan bersamanya sendiri — bukan menerima teks dari layar
 *   pembukanya, sebab teks yang disalin berikut pembaruan bundelnya tak lagi sampai ke sini.
 * - **Judulnya menyebut layar asalnya dengan nama bersama** (`NamaLayar`), bukan ejaan sendiri —
 *   satu layar satu nama, sama seperti `NamaLayarSatuTempatTest`.
 * - **Rutenya terdaftar di navigasi** tetapi bukan tujuan notifikasi: keterangan hanya bermakna
 *   bersama layar asalnya (`Routes.LANGSUNG`), dan tombol kembalinya memakai ikon mengikuti arah
 *   baca (`AutoMirrored`).
 */
class KeteranganLayarTest {

    private fun root(): File = KodeSumber.root()

    private fun kode(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(
            KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private fun layar(relatif: String): String = kode("feature/$relatif")

    /** Layar konten yang keterangannya dipindahkan ke layar keterangan. */
    private val layarBerketerangan = listOf(
        "yasin/YasinScreen.kt",
        "tahlil/TahlilScreen.kt",
        "dzikir/DzikirScreen.kt",
        "dzikir/DzikirGroupScreen.kt",
        "sholat/SholatScreen.kt",
        "sholat/SholatDetailScreen.kt",
        "sholat/WudhuScreen.kt",
        "sholat/GhaibScreen.kt",
        "sholat/JenazahScreen.kt",
        "adzan/AdzanScreen.kt",
        "adzan/IqomahDoaScreen.kt",
        "kiblat/KiblatScreen.kt",
        "jadwal/JadwalScreen.kt",
        "kalender/KalenderScreen.kt",
    )

    @Test
    fun tombolInfoDideklarasikanDiKomponenBersama() {
        val komponen = kode("core/designsystem/Komponen.kt")
        assertTrue("Komponen.kt tidak mendeklarasikan TombolInfo",
            Regex("""fun\s+TombolInfo\s*\(""").containsMatchIn(komponen))
        assertTrue("TombolInfo tidak memakai ikon Info", komponen.contains("Icons.Filled.Info"))
        // Label aksesibilitasnya dari kosakata bersama, sehingga pembaca layar menyebut pintu yang
        // sama dengan yang dijanjikan judul layar keterangannya.
        assertTrue("TombolInfo tidak menyebut labelnya dari NamaAntarmuka",
            komponen.contains("NamaAntarmuka.KETERANGAN"))
    }

    @Test
    fun setiapLayarKontenMenawarkanKeteranganDenganIkonInfo() {
        val pelanggar = layarBerketerangan.filterNot { berkas ->
            // Pemanggilan memakai lambda-ekor (`TombolInfo { … }`), jadi yang dicek kehadiran
            // namanya — bukan bentuk berkurung.
            val k = layar(berkas)
            k.contains("TombolInfo") && k.contains("Routes.keterangan(")
        }
        assertTrue("layar tanpa pintu keterangan (TombolInfo + Routes.keterangan): $pelanggar",
            pelanggar.isEmpty())
    }

    /**
     * Rincian per-sholat memakai asal `sholat:<id>` — bentuk yang dikenali layar keterangannya.
     * Tanpa ini, ikon Info pada layar rincian membuka keadaan kosong tanpa memberi tahu sebabnya.
     */
    @Test
    fun rincianSholatMenyertakanIdPadaAsalnya() {
        assertTrue("SholatDetailScreen tidak menyertakan id sholat pada asal keterangannya",
            layar("sholat/SholatDetailScreen.kt").contains("Routes.keterangan(\"sholat:\$id\")"))
    }

    @Test
    fun layarKeteranganMembacaUlangDariSumberYangSama() {
        val k = layar("keterangan/KeteranganScreen.kt")
        assertTrue("KeteranganScreen tidak memakai ContentRepo lewat rememberRepo()",
            k.contains("rememberRepo()"))
        // Parameternya hanya asal dan kembali — teks keterangan yang dikirim dari layar pembuka
        // adalah salinan yang tak lagi terbarui saat bundelnya berubah.
        assertTrue("KeteranganScreen menerima teks dari layar pembuka, bukan membaca ulang sumbernya",
            Regex("""fun\s+KeteranganScreen\s*\(\s*asal:\s*String,\s*kembali:\s*\(\)\s*->\s*Unit\s*\)""")
                .containsMatchIn(k))
    }

    /**
     * Judul layar keterangan menyusun nama layar asalnya dari kosakata bersama `NamaLayar` —
     * ejaan sendiri akan membuat satu layar punya dua nama, persis yang diperingatkan
     * `NamaLayarSatuTempatTest`.
     */
    @Test
    fun judulKeteranganMemakaiNamaLayarBersama() {
        val k = layar("keterangan/KeteranganScreen.kt")
        assertTrue("judul layar keterangan tidak memakai NamaAntarmuka.KETERANGAN",
            k.contains("NamaAntarmuka.KETERANGAN"))
        listOf(
            "NamaLayar.YASIN", "NamaLayar.TAHLIL", "NamaLayar.DZIKIR", "NamaLayar.SHOLAT",
            "NamaLayar.WUDHU", "NamaLayar.GHAIB", "NamaLayar.JENAZAH", "NamaLayar.ADZAN",
            "NamaLayar.DOA_SEBELUM_ADZAN", "NamaLayar.LAFAZ_ADZAN", "NamaLayar.DOA_SETELAH_ADZAN",
            "NamaLayar.IQOMAH", "NamaLayar.KIBLAT", "NamaLayar.JADWAL",
        ).forEach { nama ->
            assertTrue("nama layar asal $nama tidak dipetakan layar keterangan", k.contains(nama)) }
    }

    @Test
    fun ruteKeteranganTerdaftarDiNavigasiDenganKembali() {
        val nav = kode("navigation/AppNav.kt")
        assertTrue("AppNav tidak mendaftarkan rute keterangan", nav.contains("Routes.KETERANGAN"))
        assertTrue("AppNav tidak menyediakan tombol kembali dari layar keterangan",
            nav.contains("popBackStack()"))
    }

    /** Keterangan bukan tujuan notifikasi: tanpa layar asalnya, isinya tanpa konteks. */
    @Test
    fun ruteKeteranganBukanTujuanLangsungNotifikasi() {
        val rute = kode("navigation/Routes.kt")
        assertTrue("Routes tidak menyediakan pembentuk rute keterangan",
            rute.contains("fun keterangan(asal: String): String"))
        val langsung = rute.substringAfter("val LANGSUNG").substringBefore(")")
        assertFalse("rute keterangan ikut menjadi tujuan notifikasi padahal butuh konteks layar asal",
            langsung.contains("KETERANGAN"))
    }

    /** Ikon panah kembali mengikuti arah baca — bukan `Icons.Filled.ArrowBack` yang usang. */
    @Test
    fun ikonKembaliMengikutiArahBaca() {
        val k = layar("keterangan/KeteranganScreen.kt")
        assertTrue("tombol kembali tidak memakai varian AutoMirrored",
            k.contains("AutoMirrored.Filled.ArrowBack"))
        assertFalse("tombol kembali memakai ikon panah usang yang tak terbalik pada tata letak RTL",
            k.contains("Icons.Filled.ArrowBack"))
    }
}
