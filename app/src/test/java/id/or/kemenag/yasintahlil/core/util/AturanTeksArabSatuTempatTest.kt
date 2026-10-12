package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan teks Arab ([TeksArab]) harus tetap **satu** — dipakai aplikasi saat memasang bundel, uji
 * assets saat memeriksa konten bawaan, *dan* gate konten di CI.
 *
 * Aturan ini pernah ditulis **tiga kali** dengan nilai yang sama:
 *
 *  - `BundelKonten` — saat bundel pembaruan yang dipilih pengguna dipasang;
 *  - `ArabicTextTest` — saat seluruh assets diperiksa di CI;
 *  - `tools/validate_content.py` — gate konten yang menilai berkas yang sama saat dibangun.
 *
 * Ketiganya mengeja blok Arab yang sama (`0600–06FF`, `0750–077F`), rentang huruf yang sama
 * (`0621–064A`, `0671–06D3`), tanda harakat yang sama (`064B–0652` ditambah lima kode tunggal), dan
 * panjang kata minimum yang sama. Tak satu pun membandingkan dirinya dengan yang lain.
 *
 * Salinan seperti ini lebih berbahaya daripada salinan biasa karena **rentang Unicode tidak dapat
 * dibaca sekilas**: menambah satu tanda baris baru di satu tempat tidak membuat berkas mana pun
 * tampak berubah bagi peninjau. Bila gate Python tertinggal, teks yang belum divokalkan lolos CI
 * lalu **ditampilkan kepada pengguna sebagai bacaan** — tepat yang dilarang aturan "jangan karang
 * teks agama". Bila salinan Kotlin yang tertinggal, uji assets gagal pada berkas yang justru sah,
 * dan perbaikan termurah adalah menambal satu salinan — persis cara salinan ketiga lahir.
 *
 * Karena itu nilainya kini tinggal di [TeksArab], dan gate Python membacanya dari berkas itu.
 * Uji ini menjaga agar tidak ada salinan baru, dan agar pola pengambilnya benar-benar menghasilkan
 * nilai aplikasi — sama seperti `SumberResmiSatuTempatTest` untuk daftar host resmi.
 */
class AturanTeksArabSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    /**
     * Akar repositori, tempat `app/` dan `tools/` bersebelahan.
     *
     * Uji berjalan dengan direktori kerja modul (`app/`), jadi [root] menunjuk ke `app` — naik satu
     * tingkat dulu sebelum mencari gate.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${root().absolutePath}")

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val berkasTeksArab = "src/main/java/id/or/kemenag/yasintahlil/core/util/TeksArab.kt"

    private val gerbangKonten: String
        get() = File(repoRoot(), "tools/validate_content.py").readText()

    /** Pola pengambil yang ditulis **persis sama** di gate Python. */
    private val polaRentang = """0x([0-9A-Fa-f]+)\s+to\s+0x([0-9A-Fa-f]+)"""
    private val polaKode = """0x([0-9A-Fa-f]+)"""

    private val polaList = """\b%s\s*=\s*listOf\(([^)]*)\)"""

    private fun isiList(sumber: String, nama: String): String =
        Regex(polaList.format(nama)).find(sumber)?.groupValues?.get(1)
            ?: error("'$nama = listOf(...)' tidak ditemukan di $berkasTeksArab")

    private fun rentang(sumber: String, nama: String): List<Pair<Int, Int>> =
        Regex(polaRentang).findAll(isiList(sumber, nama))
            .map { it.groupValues[1].toInt(16) to it.groupValues[2].toInt(16) }.toList()

    private fun kode(sumber: String, nama: String): List<Int> =
        Regex(polaKode).findAll(isiList(sumber, nama))
            .map { it.groupValues[1].toInt(16) }.toList()

    private fun angka(sumber: String, nama: String): Int =
        Regex("""\b$nama\s*=\s*(\d+)""").find(sumber)?.groupValues?.get(1)?.toInt()
            ?: error("'$nama = <angka>' tidak ditemukan di $berkasTeksArab")

    // --- Satu tempat ----------------------------------------------------------------------------

    /**
     * Tidak ada berkas Kotlin lain yang mengeja rentang atau kode Arabnya.
     *
     * Ditulis dalam kedua bentuk yang mungkin: kode heksadesimal (`0x0600`) dan escape di dalam
     * literal string (`؀`), sebab keduanya sama-sama menyatakan aturan yang sama.
     */
    @Test
    fun aturanTeksArabHanyaDinyatakanDiTeksArab() {
        val kodeHex = (TeksArab.RENTANG_ARAB + TeksArab.RENTANG_HURUF + TeksArab.RENTANG_HARAKAT)
            .flatMap { (a, b) -> listOf(a, b) } + TeksArab.HARAKAT_TUNGGAL
        val tanda = kodeHex.flatMap {
            listOf("0x%04X".format(it), "0x%04x".format(it), "\\u%04X".format(it), "\\u%04x".format(it))
        }.distinct()

        val pelanggar = mutableListOf<String>()
        berkasMain().filter { it.name != "TeksArab.kt" }.forEach { f ->
            val kode = KodeSumber.kodeTanpaKomentar(f.readText())
            tanda.filter { kode.contains(it) }.forEach { pelanggar += "${f.name}: $it" }
        }
        assertTrue("aturan teks Arab dieja ulang di berkas yang tidak dijaga: $pelanggar",
            pelanggar.isEmpty())
    }

    /** Kedua pemakainya benar-benar membaca [TeksArab], bukan menghitung ulang sendiri. */
    @Test
    fun pemakaiMembacaAturanBersama() {
        val bundel = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/data/BundelKonten.kt"))
        assertTrue("BundelKonten tidak memakai pola Arab bersama",
            bundel.contains("TeksArab.regexArab"))
        assertTrue("BundelKonten tidak memakai pemeriksa harakat bersama",
            bundel.contains("TeksArab.tanpaHarakat("))

        val ujiAssets = KodeSumber.kodeTanpaKomentar(
            File(root(), "src/test/java/id/or/kemenag/yasintahlil/ArabicTextTest.kt").readText())
        assertTrue("uji assets tidak memakai pemeriksa harakat bersama",
            ujiAssets.contains("TeksArab.tanpaHarakat("))
        assertTrue("uji assets tidak memakai pola Arab bersama",
            ujiAssets.contains("TeksArab.regexArab"))
    }

    // --- Gate membaca aturan yang sama ----------------------------------------------------------

    @Test
    fun gerbangKontenMembacaAturanDariTeksArab() {
        assertTrue("gate konten tidak membaca aturan teks Arab dari TeksArab.kt",
            gerbangKonten.contains("TeksArab.kt"))
        assertTrue("gate konten memakai pola rentang yang berbeda dari penjaganya",
            gerbangKonten.contains(polaRentang))
        assertTrue("gate konten memakai pola kode yang berbeda dari penjaganya",
            gerbangKonten.contains(polaKode))
        // Salinan rentang di gate akan membuat dua kebijakan yang dapat berbeda tanpa galat.
        val salinan = listOf("0x0600", "0x06FF", "0x0621", "0x064A", "0x064B", "0x0652", "0x0670")
            .filter { gerbangKonten.contains(it) }
        assertTrue("gate konten menyimpan salinan rentang Arab: $salinan", salinan.isEmpty())
        assertTrue("gate konten tidak memakai panjang kata minimum bersama",
            gerbangKonten.contains("MIN_HURUF"))
    }

    /** Pola pengambil benar-benar menghasilkan aturan aplikasi — bukan sekadar cocok. */
    @Test
    fun polaPengambilMenghasilkanAturanAplikasi() {
        val sumber = KodeSumber.sumber(berkasTeksArab)

        assertEquals("rentang Arab yang dibaca gate berbeda dari yang dipakai aplikasi",
            TeksArab.RENTANG_ARAB, rentang(sumber, "RENTANG_ARAB"))
        assertEquals("rentang huruf yang dibaca gate berbeda dari yang dipakai aplikasi",
            TeksArab.RENTANG_HURUF, rentang(sumber, "RENTANG_HURUF"))
        assertEquals("rentang harakat yang dibaca gate berbeda dari yang dipakai aplikasi",
            TeksArab.RENTANG_HARAKAT, rentang(sumber, "RENTANG_HARAKAT"))
        assertEquals("kode harakat tunggal yang dibaca gate berbeda dari yang dipakai aplikasi",
            TeksArab.HARAKAT_TUNGGAL, kode(sumber, "HARAKAT_TUNGGAL"))
        assertEquals(TeksArab.MIN_HURUF, angka(sumber, "MIN_HURUF"))

        // Daftar kosong akan membuat gate meloloskan segalanya atau menolak segalanya.
        assertTrue("rentang Arab kosong", TeksArab.RENTANG_ARAB.isNotEmpty())
        assertTrue("rentang huruf kosong", TeksArab.RENTANG_HURUF.isNotEmpty())
        assertTrue("rentang harakat kosong", TeksArab.RENTANG_HARAKAT.isNotEmpty())
        assertTrue("kode harakat tunggal kosong", TeksArab.HARAKAT_TUNGGAL.isNotEmpty())
        assertTrue("panjang kata minimum tidak masuk akal", TeksArab.MIN_HURUF >= 1)
        // Harakat yang kosong membuat setiap kata Arab dianggap tanpa baris — seluruh konten
        // berharakat akan ditolak.
        assertTrue("himpunan harakat kosong", TeksArab.HARAKAT.isNotEmpty())
    }

    // --- Aturannya benar-benar berlaku ----------------------------------------------------------

    /**
     * Aturan bersama benar-benar menilai seperti yang dijanjikan KDoc-nya.
     *
     * Tanpa ini, [TeksArab] dapat "dipakai bersama" oleh ketiga sisi sambil tidak menolak apa pun —
     * sepakat pada aturan yang tidak berfungsi.
     */
    @Test
    fun aturanBersamaBenarBenarMenilai() {
        // Teks berharakat: tidak ada yang dilaporkan.
        assertEquals("teks berharakat dituduh tanpa harakat",
            emptyList<String>(), TeksArab.tanpaHarakat("أَسْتَغْفِرُ اللهَ"))

        // Teks mentah yang belum divokalkan: dilaporkan (contoh yang sudah tercatat di repositori
        // ini sebagai kekeliruan lama pada `adzan.json`).
        val mentah = TeksArab.tanpaHarakat("الصلاة خير من النوم")
        assertTrue("teks Arab tanpa harakat tidak terdeteksi", mentah.isNotEmpty())

        // Kata yang lebih pendek dari MIN_HURUF tidak dinilai — menilainya hanya menghasilkan
        // galat palsu pada kata sambung.
        assertEquals("kata di bawah panjang minimum ikut dinilai",
            emptyList<String>(), TeksArab.tanpaHarakat("من"))
    }

    /** Aturan ini murni: tanpa Android, sehingga dapat diuji langsung di JVM. */
    @Test
    fun aturanTeksArabTidakMenyentuhAndroid() {
        val kode = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(berkasTeksArab))
        assertFalse("aturan teks Arab menyentuh Android", kode.contains("import android."))
    }
}
