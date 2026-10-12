package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Jenis bacaan tersimpan — bagian sebelum `:` pada id bacaan — hanya punya satu ejaan.
 *
 * Jenis ini menyambung **penulis** dan **pembaca**: layar menulisnya sebagai awalan id kartu yang
 * disimpan `BacaanCard` ke favorit, riwayat, dan progres; `RuteKonten` membacanya kembali untuk
 * menentukan layar tujuan tombol "Buka". Sebelumnya kedua sisi mengejanya sendiri-sendiri —
 * `"yasin"` di `YasinScreen` dan di `RuteKonten`, `"adzan"` di layar Adzan dan di `RuteKonten` —
 * sehingga satu huruf yang berubah di salah satu sisi membuat favorit terbuka ke layar yang salah,
 * atau berakhir "Layar tujuan tidak dikenal", tanpa satu pun pesan galat.
 *
 * Penjaga di sini bekerja dengan memindai **kode tanpa komentar** (`KodeSumber`), sebab KDoc yang
 * menjelaskan pola lama memuat tulisan yang sama persis dengan yang dilarang.
 */
class JenisBacaanSatuTempatTest {

    private val akar = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")

    private fun berkasMain(): List<File> =
        akar.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun kodeLayar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    /** Nama berkas (selain yang dikecualikan) yang memuat [literal] di dalam **kodenya**. */
    private fun berkasDengan(literal: String, vararg dikecualikan: String): List<String> =
        berkasMain()
            .filter { it.name !in dikecualikan }
            .filter { kode(it).contains(literal) }
            .map { it.name }
            .sorted()

    // ---- Menyusun dan membaca id ------------------------------------------------------------

    @Test
    fun idBacaanDisusunDariJenisDanRujukan() {
        assertEquals("yasin:58", JenisBacaan.id(JenisBacaan.YASIN, "58"))
        assertEquals("adzan:3", JenisBacaan.id(JenisBacaan.ADZAN, "3"))
        // Rujukan boleh berupa id gabungan lagi (varian niat di dalam modul sholat khusus).
        assertEquals("ghaib:niat:syafii",
            JenisBacaan.id(JenisBacaan.GHAIB, JenisBacaan.id(JenisBacaan.NIAT, "syafii")))
    }

    @Test
    fun pisahMembalikkanPenyusunanIdUntukSetiapJenis() {
        JenisBacaan.BERPETA.forEach { jenis ->
            assertEquals("jenis '$jenis' tidak terbaca kembali dari idnya",
                jenis to "rujukan-uji", JenisBacaan.pisah(JenisBacaan.id(jenis, "rujukan-uji")))
        }
    }

    @Test
    fun idTanpaJenisYangSahDibacaSebagaiJenisLain() {
        assertEquals(JenisBacaan.LAIN to "58", JenisBacaan.pisah("58"))
        assertEquals(JenisBacaan.LAIN to ":58", JenisBacaan.pisah(":58"))
    }

    /**
     * Nilai `Store.LAST` dari versi aplikasi yang lebih dahulu hanya berisi jenisnya saja; dibaca
     * sebagai jenisnya, bukan "lain" — kalau tidak, bacaan terakhir kehilangan tombol lanjutannya.
     */
    @Test
    fun nilaiTerakhirYangBelumBerpemisahTetapTerbacaJenisnya() {
        assertEquals(JenisBacaan.YASIN, JenisBacaan.jenisDari(JenisBacaan.id(JenisBacaan.YASIN, "58")))
        assertEquals(JenisBacaan.YASIN, JenisBacaan.jenisDari(JenisBacaan.YASIN))
        assertEquals("", JenisBacaan.jenisDari(""))
    }

    // ---- Kosakata jenis ----------------------------------------------------------------------

    /**
     * Ejaan jenis adalah **data tersimpan**, bukan sekadar nama internal: id kartu ditulis ke
     * favorit, riwayat, dan progres lewat Room, dan baris itu bertahan melintasi pembaruan aplikasi.
     * Mengganti satu huruf di sini memutus setiap baris lama dari jenisnya — favorit dzikir berhenti
     * terbuka, progres Tahlil kembali ke nol — tanpa galat apa pun. Karena itu ejaannya dikunci di
     * sini: mengubahnya harus disengaja, dan harus disertai migrasi.
     */
    @Test
    fun ejaanJenisTersimpanTidakBerubah() {
        assertEquals(
            mapOf(
                "YASIN" to "yasin", "TAHLIL" to "tahlil", "DZIKIR" to "dzikir",
                "DOA" to "doa", "IQOMAH" to "iqomah", "ADZAN" to "adzan",
                "BACAAN" to "bacaan", "NIAT" to "niat", "LANGKAH" to "langkah",
                "GHAIB" to "ghaib", "JENAZAH" to "jenazah", "SEARCH" to "search",
                "SHOLAT" to "sholat", "WUDHU" to "wudhu", "LAIN" to "lain",
            ),
            mapOf(
                "YASIN" to JenisBacaan.YASIN, "TAHLIL" to JenisBacaan.TAHLIL,
                "DZIKIR" to JenisBacaan.DZIKIR, "DOA" to JenisBacaan.DOA,
                "IQOMAH" to JenisBacaan.IQOMAH, "ADZAN" to JenisBacaan.ADZAN,
                "BACAAN" to JenisBacaan.BACAAN, "NIAT" to JenisBacaan.NIAT,
                "LANGKAH" to JenisBacaan.LANGKAH, "GHAIB" to JenisBacaan.GHAIB,
                "JENAZAH" to JenisBacaan.JENAZAH, "SEARCH" to JenisBacaan.SEARCH,
                "SHOLAT" to JenisBacaan.SHOLAT, "WUDHU" to JenisBacaan.WUDHU,
                "LAIN" to JenisBacaan.LAIN,
            ),
        )
        // Pemisahnya pun bagian dari format tersimpan: `yasin:58` di penyimpanan lama.
        assertEquals(':', JenisBacaan.PEMISAH)
    }

    @Test
    fun kosakataJenisTidakTumpangTindih() {
        assertEquals("jenis pada BERPETA tidak unik",
            JenisBacaan.BERPETA.size, JenisBacaan.BERPETA.toSet().size)
        assertTrue("jenis progres ikut masuk BERPETA",
            JenisBacaan.HANYA_PROGRES.none { it in JenisBacaan.BERPETA })
        assertTrue("jenis progres tidak unik",
            JenisBacaan.HANYA_PROGRES.size == JenisBacaan.HANYA_PROGRES.toSet().size)
        assertTrue("jenis `lain` ikut masuk BERPETA", JenisBacaan.LAIN !in JenisBacaan.BERPETA)
    }

    // ---- Tidak ada yang mengeja jenisnya sendiri ---------------------------------------------

    /** Penulis id kartu memakai [JenisBacaan.id]; tidak ada literal `"<jenis>:` di luar kosakatanya. */
    @Test
    fun jenisTidakDiejaSebagaiAwalanIdDiLuarKosakatanya() {
        val pelanggar = JenisBacaan.BERPETA.flatMap { jenis -> berkasDengan("\"$jenis:") }
        assertTrue("id bacaan menyusun jenisnya sendiri di luar `JenisBacaan`: $pelanggar",
            pelanggar.isEmpty())
    }

    /** Jenis yang disimpan sebagai argumen juga datang dari kosakata, bukan literal. */
    @Test
    fun jenisTidakDiberikanSebagaiLiteralKePenyimpanan() {
        val panggil = listOf(
            "bookmarks(", "progress(", "progressByKind(", "setProgress(",
            "pushHistory(", "toggleBookmark(", "toggleFavorite(",
        )
        val pelanggar = JenisBacaan.BERPETA
            .flatMap { jenis -> panggil.map { "$it\"$jenis\"" } }
            .flatMap { bentuk -> berkasDengan(bentuk) }
        assertTrue("jenis bacaan diberikan sebagai literal, bukan dari `JenisBacaan`: $pelanggar",
            pelanggar.isEmpty())
    }

    @Test
    fun petaJenisKeLayarMemakaiKosakataBersama() {
        val peta = kodeLayar("navigation/RuteKonten.kt")
        assertTrue("RuteKonten tidak memakai `JenisBacaan`", peta.contains("JenisBacaan."))
        val dieja = JenisBacaan.BERPETA.filter { peta.contains("\"$it\"") }
        assertTrue("RuteKonten mengeja jenis sendiri: $dieja", dieja.isEmpty())
    }

    @Test
    fun pembacaIdGabunganMemakaiAturanBersama() {
        assertTrue("splitContentId tidak memakai aturan pemisah bersama",
            kodeLayar("core/designsystem/HiltAccess.kt").contains("JenisBacaan.pisah("))
        assertTrue("migrasi favorit warisan tidak memakai aturan pemisah bersama",
            kodeLayar("YasinApp.kt").contains("JenisBacaan.pisah("))
    }

    /** Pemisahnya tidak disalin: satu tempat memutuskan bentuk id, yang lain memakainya. */
    @Test
    fun pemisahIdBacaanTidakDisalinKeTempatLain() {
        val penyalin = berkasDengan("indexOf(':')") + berkasDengan("substringBefore(':')")
        assertTrue("pemisah id bacaan disalin ke luar `JenisBacaan`: $penyalin", penyalin.isEmpty())
    }

    /** Layar yang menyimpan jenis membacanya dari kosakata bersama, bukan dari salinan lokal. */
    @Test
    fun layarYangMenulisJenisMemakainyaDariKosakataBersama() {
        val penulis = mapOf(
            "feature/yasin/YasinViewModel.kt" to "JenisBacaan.YASIN",
            "feature/tahlil/TahlilViewModel.kt" to "JenisBacaan.TAHLIL",
            "feature/dzikir/DzikirViewModel.kt" to "JenisBacaan.DZIKIR",
            "feature/sholat/SholatDetailScreen.kt" to "JenisBacaan.SHOLAT",
            "feature/sholat/WudhuScreen.kt" to "JenisBacaan.WUDHU",
            "feature/search/SearchScreen.kt" to "JenisBacaan.SEARCH",
        )
        penulis.forEach { (berkas, konstanta) ->
            assertTrue("$berkas tidak memakai $konstanta", kodeLayar(berkas).contains(konstanta))
        }
    }
}
