package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.sholat.Mazhab
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.JudulBacaan
import id.or.kemenag.yasintahlil.navigation.RuteKonten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Sebutan untuk hal yang sama harus satu ejaan.
 *
 * Empat sebutan yang dahulu ditulis di beberapa tempat sekaligus:
 *
 *  - **nama berkas modul sholat khusus** (`sholat_ghaib.json`, `sholat_jenazah.json`) — di layar
 *    Ghaib, layar Jenazah, dan indeks pencarian. Nama berkas adalah kunci ke bundel konten, jadi
 *    salah ketik di satu tempat membuat layar menampilkan "konten tidak tersedia" sementara
 *    pencarian tetap mendaftarkannya;
 *  - **judul satu ayat Yasin** (`Yasin ayat 58`) — di `YasinViewModel` (riwayat & favorit) dan di
 *    indeks pencarian, sehingga ayat yang sama dapat muncul dengan dua nama;
 *  - **nama mazhab Asar** beserta faktor bayangannya (`Syafi'i (1)`) — di menu Jadwal dan menu
 *    Pengaturan, yang keduanya mengatur nilai yang sama (`Store.ASAR`).
 *  - **id entri daftar sholat khusus** (`ghaib`, `jenazah`) — di `sholat_daftar.json` dan di layar
 *    Panduan Sholat, yang menawarkan tombol "Buka" bagi entri dengan id itu.
 */
class SebutanSatuTempatTest {

    private val akar = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")

    private fun berkasSelain(vararg dikecualikan: String): List<File> =
        akar.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name !in dikecualikan }.toList()

    /** Kode [f] tanpa komentar; string dipertahankan supaya literal yang dilarang tetap terbaca. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun layar(relatif: String): String =
        KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif")

    /**
     * Kode layar tanpa komentar: penjaga "layar ini memanggil aturan bersama" harus membaca
     * **kode**, bukan KDoc. KDoc di berkas yang sama menyebut `BerkasKonten.SHOLAT_GHAIB` untuk
     * menjelaskan dari mana namanya datang, sehingga penjaga yang membaca mentah akan tetap hijau
     * meski panggilan aslinya sudah mengeja namanya sendiri.
     */
    private fun kodeLayar(relatif: String): String = KodeSumber.kodeTanpaKomentar(layar(relatif))

    private fun berkasDengan(literal: String, vararg dikecualikan: String): List<String> =
        berkasSelain(*dikecualikan).filter { kode(it).contains(literal) }.map { it.name }

    private fun dirKonten(): File = listOf(File("src/main/assets/content"), File("app/src/main/assets/content"))
        .firstOrNull { it.isDirectory } ?: error("Direktori assets/content tidak ditemukan")

    // ---- Nama berkas modul sholat khusus ---------------------------------------------------

    @Test
    fun namaBerkasSholatKhususHanyaDiejaDiBerkasKonten() {
        val pelanggar = berkasDengan("sholat_ghaib.json", "BerkasKonten.kt") +
            berkasDengan("sholat_jenazah.json", "BerkasKonten.kt")
        assertTrue("nama berkas modul sholat khusus ditulis ulang di luar BerkasKonten: $pelanggar",
            pelanggar.isEmpty())
    }

    @Test
    fun berkasYangDisebutBerkasKontenBenarBenarAdaDiBundel() {
        val hilang = BerkasKonten.SHOLAT_KHUSUS.map { it.berkas }
            .filterNot { File(dirKonten(), it).isFile }
        assertTrue("berkas yang disebut BerkasKonten tidak ada di assets/content: $hilang", hilang.isEmpty())
    }

    @Test
    fun layarSholatKhususDanPencarianMemakaiDaftarYangSama() {
        assertTrue("layar Ghaib tidak membaca nama berkas dari BerkasKonten",
            kodeLayar("feature/sholat/GhaibScreen.kt").contains("BerkasKonten.SHOLAT_GHAIB"))
        assertTrue("layar Jenazah tidak membaca nama berkas dari BerkasKonten",
            kodeLayar("feature/sholat/JenazahScreen.kt").contains("BerkasKonten.SHOLAT_JENAZAH"))
        assertTrue("indeks pencarian tidak memakai daftar modul yang sama",
            kodeLayar("feature/search/SearchScreen.kt").contains("BerkasKonten.SHOLAT_KHUSUS"))
    }

    @Test
    fun setiapModulSholatKhususPunyaLayarSendiri() {
        // Rute tiap modul harus berbeda: dua modul yang menunjuk layar sama berarti salah satunya
        // membuka bacaan yang lain — tombol yang membawa ke tempat salah.
        val rute = BerkasKonten.SHOLAT_KHUSUS.map { it.rute }
        assertEquals("rute modul sholat khusus tidak unik", rute.size, rute.toSet().size)
        assertTrue("modul sholat khusus tanpa rute", rute.none { it.isBlank() })
    }

    /**
     * Nama yang dipakai tombol "Buka" di Panduan Sholat harus nama yang **sama** dengan yang
     * diberikan peta bersama `RuteKonten` untuk rute itu.
     *
     * Tombol yang menyebut layar tujuan dengan namanya sendiri dapat menyebut layar yang lain: nama
     * yang tertukar di [BerkasKonten.SHOLAT_KHUSUS] membuat entri "Ghaib" menawarkan tombol
     * *"Buka Sholat Jenazah"* yang, begitu ditekan, membuka layar Ghaib — persis kebingungan yang
     * dicegah berkas `NamaLayar`. Peta rute → nama sudah ada satu di `RuteKonten`, jadi yang
     * diuji di sini bukan ejaannya, melainkan **kesepakatannya**.
     */
    @Test
    fun namaModulSholatKhususSelarasDenganPetaRuteKeNama() {
        val pelanggar = BerkasKonten.SHOLAT_KHUSUS.mapNotNull { m ->
            val jenis = JenisBacaan.BERPETA.filter { RuteKonten.rute(it) == m.rute }
            val nama = jenis.mapNotNull { RuteKonten.namaTujuan(it) }.distinct()
            when {
                nama.size != 1 -> "rute ${m.rute}: tak ada nama tunggal di RuteKonten ($nama)"
                nama.single() != m.nama -> "rute ${m.rute}: modul menulis \"${m.nama}\", " +
                    "RuteKonten menulis \"${nama.single()}\""
                else -> null
            }
        }
        assertTrue("nama modul sholat khusus tidak selaras dengan RuteKonten: $pelanggar",
            pelanggar.isEmpty())
    }

    /**
     * Panduan Sholat mengenali entri yang punya modul sendiri dari daftar bersama, bukan dari id
     * yang dieja di dalam layarnya.
     *
     * Sebelumnya layar itu menulis `s.id == "ghaib"` / `s.id == "jenazah"`, dan tak ada apa pun yang
     * menyandingkan kedua id itu dengan `sholat_daftar.json`. Menggantinya di bundel karena itu
     * menghapus tombol "Buka" **tanpa satu pun pesan galat** — entri itu jatuh ke tombol "Tata cara"
     * yang membuka rincian tanpa isi. Menambah modul khusus pun tak menolong: yang baru tidak akan
     * pernah dikenali, betapa pun lengkapnya barisnya di [BerkasKonten.SHOLAT_KHUSUS].
     */
    @Test
    fun layarPanduanSholatMembacaDaftarModulKhusus() {
        val k = kodeLayar("feature/sholat/SholatScreen.kt")
        assertTrue("layar Panduan Sholat tidak memakai BerkasKonten.SHOLAT_KHUSUS",
            k.contains("BerkasKonten.SHOLAT_KHUSUS"))
        val dieja = BerkasKonten.SHOLAT_KHUSUS.map { it.idDaftar }.filter { k.contains("\"$it\"") }
        assertTrue(
            "layar Panduan Sholat mengeja id entri daftar alih-alih membacanya dari " +
                "BerkasKonten.SHOLAT_KHUSUS: $dieja",
            dieja.isEmpty(),
        )
    }

    // ---- Judul ayat Yasin ------------------------------------------------------------------

    @Test
    fun judulAyatYasinHanyaDiejaDiJudulBacaan() {
        val pelanggar = berkasDengan("Yasin ayat", "JudulBacaan.kt")
        assertTrue("judul ayat Yasin ditulis ulang di luar JudulBacaan: $pelanggar", pelanggar.isEmpty())
    }

    @Test
    fun yasinAyatMenyusunJudulDenganNomornya() {
        assertEquals("Yasin ayat 58", JudulBacaan.yasinAyat(58))
        assertEquals("Yasin ayat 1", JudulBacaan.yasinAyat(1))
    }

    @Test
    fun penulisRiwayatDanIndeksPencarianMemakaiJudulBacaan() {
        assertTrue("YasinViewModel tidak memakai JudulBacaan",
            kodeLayar("feature/yasin/YasinViewModel.kt").contains("JudulBacaan.yasinAyat("))
        assertTrue("indeks pencarian tidak memakai JudulBacaan",
            kodeLayar("feature/search/SearchScreen.kt").contains("JudulBacaan.yasinAyat("))
    }

    // ---- Mazhab Asar ----------------------------------------------------------------------

    @Test
    fun namaMazhabAsarDanFaktornyaDariAturanBersama() {
        assertEquals("Syafi'i", JadwalHariIni.asarLabel(false))
        assertEquals("Hanafi", JadwalHariIni.asarLabel(true))
        // Angka dalam tanda kurung adalah faktor bayangan yang benar-benar dipakai perhitungan.
        assertEquals("Syafi'i (1)", JadwalHariIni.asarLabelFaktor(false))
        assertEquals("Hanafi (2)", JadwalHariIni.asarLabelFaktor(true))
        assertEquals(JadwalHariIni.ASAR_SYAFII, JadwalHariIni.asarKey(false))
        assertEquals(JadwalHariIni.ASAR_HANAFI, JadwalHariIni.asarKey(true))
    }

    @Test
    fun tidakAdaYangMenulisUlangNamaMazhabAsarAtauFaktornya() {
        val pelanggar = berkasDengan("Syafi'i (1)", "JadwalHariIni.kt") +
            berkasDengan("Hanafi (2)", "JadwalHariIni.kt")
        assertTrue("nama mazhab Asar beserta faktornya ditulis ulang di luar JadwalHariIni: $pelanggar",
            pelanggar.isEmpty())
    }

    @Test
    fun kunciMazhabAsarTidakDitulisSebagaiLiteral() {
        // Kuncinya (`"syafii"`/`"hanafi"`) hanya boleh datang dari konstanta bersama; literal di
        // `store.set(Store.ASAR, "…")` adalah salinan yang tidak dijaga apa pun.
        val pelanggar = berkasDengan("Store.ASAR, \"")
        assertTrue("kunci mazhab Asar ditulis sebagai literal: $pelanggar", pelanggar.isEmpty())
    }

    @Test
    fun keduaLayarMenawarkanPilihanMazhabAsarYangSama() {
        assertTrue("menu Jadwal tidak memakai nama mazhab dari aturan bersama",
            kodeLayar("feature/jadwal/JadwalScreen.kt").contains("JadwalHariIni.asarLabel("))
        assertTrue("menu Pengaturan tidak memakai daftar pilihan mazhab Asar bersama",
            kodeLayar("feature/settings/SettingsScreen.kt").contains("JadwalHariIni.ASAR_SEMUA"))
        assertTrue("pasangan boolean→kunci mazhab Asar disusun ulang di JadwalViewModel",
            kodeLayar("feature/jadwal/JadwalViewModel.kt").contains("JadwalHariIni.asarKey("))
    }

    @Test
    fun mengubahMazhabAsarDariPengaturanIkutMenjadwalkanUlangPengingat() {
        // Mazhab Asar menggeser jam Asar; menyimpan nilainya tanpa menyusun ulang pengingat membuat
        // notifikasi tetap memakai mazhab lama. Menu Jadwal sudah melakukannya sejak dulu.
        val blok = kodeLayar("feature/settings/SettingsScreen.kt")
            .substringAfter("JadwalHariIni.ASAR_SEMUA.forEach")
            .take(700)
        assertTrue("mengubah mazhab Asar di menu Pengaturan tidak menjadwalkan ulang pengingat",
            blok.contains("PrayerScheduler.scheduleAll"))
    }

    /**
     * Kosakata mazhab Asar harus **dipinjam** dari kosakata mazhab panduan, bukan disalin.
     *
     * Layar Pengaturan menampilkan kedua daftar itu berdampingan (`Mazhab.SEMUA` untuk mazhab
     * panduan, `JadwalHariIni.ASAR_SEMUA` untuk mazhab Asar), dan keduanya menyebut mazhab yang
     * sama. Dulu tiap daftar menuliskan kunci dan namanya sendiri, sehingga mengubah nama di
     * [Mazhab] menyisakan layar itu menawarkan dua sebutan untuk satu mazhab — tanpa satu pun galat.
     */
    @Test
    fun kosakataMazhabAsarDipinjamDariKosakataMazhabPanduan() {
        assertEquals("kunci mazhab Asar Syafi'i berbeda dari mazhab panduan",
            Mazhab.SYAFII, JadwalHariIni.ASAR_SYAFII)
        assertEquals("kunci mazhab Asar Hanafi berbeda dari mazhab panduan",
            Mazhab.HANAFI, JadwalHariIni.ASAR_HANAFI)
        assertEquals("mazhab Asar bawaan berbeda dari mazhab panduan bawaan",
            Mazhab.DEFAULT, JadwalHariIni.ASAR_DEFAULT)

        val dariPanduan = Mazhab.SEMUA.filter {
            it.first == JadwalHariIni.ASAR_SYAFII || it.first == JadwalHariIni.ASAR_HANAFI
        }
        assertEquals("pilihan mazhab Asar bukan bagian dari daftar mazhab panduan",
            dariPanduan, JadwalHariIni.ASAR_SEMUA)
        // Tepat dua mazhab yang jam Asarnya berbeda: daftar yang menyusut membuat `asarLabel`
        // melempar, dan daftar yang memanjang menawarkan mazhab yang tak mengubah perhitungan.
        assertEquals("pilihan mazhab Asar tidak lagi tepat dua", 2, JadwalHariIni.ASAR_SEMUA.size)
    }

    /**
     * Kunci dan nama mazhab Asar tak boleh dieja kembali di [JadwalHariIni].
     *
     * Nilai yang kebetulan sama hari ini bukan jaminan: salinan yang ditulis ulang akan lepas lagi
     * begitu salah satunya diubah. Karena itu yang diperiksa adalah **ejaannya**, bukan hasilnya.
     */
    @Test
    fun JadwalHariIniTidakMengejaUlangKunciDanNamaMazhab() {
        val kode = KodeSumber.kodeTanpaKomentar(
            File(
                KodeSumber.root(),
                "src/main/java/id/or/kemenag/yasintahlil/core/util/JadwalHariIni.kt",
            ).readText(),
        )
        listOf("\"syafii\"", "\"hanafi\"", "\"Syafi'i\"", "\"Hanafi\"").forEach { literal ->
            assertFalse(
                "JadwalHariIni.kt mengeja ulang $literal alih-alih meminjamnya dari Mazhab",
                kode.contains(literal),
            )
        }
    }
}
