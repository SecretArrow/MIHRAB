package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.KunciProgres
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rujukan baris progres — bagian kedua pada pasangan `(jenis, rujukan)` — tidak boleh diketik ulang
 * di tempat pemakaian.
 *
 * Baris progres ditulis dan dibaca oleh berkas yang berbeda: `TahlilViewModel` menyimpan posisi dan
 * hitungan tasbih, sementara `SettingsScreen` mengosongkan keduanya saat pengguna mengganti susunan.
 * Keduanya harus menyebut baris yang **sama**; mengetik `"idx"` di satu sisi dan `"index"` di sisi
 * lain membuat "mulai dari awal" menulis baris baru yang tidak dibaca siapa pun, sementara posisi
 * lama tetap utuh — pengguna menekan tombolnya dan penghitung tidak bergerak, tanpa galat apa pun.
 * Hal yang sama berlaku pada tanda "sudah dipelajari" di panduan wudhu, yang jenisnya sudah datang
 * dari `JenisBacaan` tetapi rujukannya dahulu literal di tiga tempat.
 *
 * Karena itu aturannya bukan "ketiga nilai ini sama di mana-mana" melainkan lebih tegas: **tidak ada
 * satu pun pemanggilan `progress`/`setProgress` yang boleh memberi rujukan berupa literal**. Rujukan
 * yang berupa id konten (mis. id sholat) tetap boleh — ia variabel, bukan ejaan yang disalin.
 */
class KunciProgresSatuTempatTest {

    private val akar = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")

    private fun berkasMain(): List<File> =
        akar.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun kodeLayar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    // ---- Pemindai pemanggilan ----------------------------------------------------------------

    /** Argumen tiap pemanggilan `nama(` di dalam [kode], dipisah pada koma teratas. */
    private fun argumen(kode: String, nama: String): List<List<String>> {
        val hasil = mutableListOf<List<String>>()
        var i = 0
        while (true) {
            val m = kode.indexOf("$nama(", i)
            if (m < 0) break
            val mulai = m + nama.length + 1
            var j = mulai
            var depth = 1
            while (j < kode.length && depth > 0) {
                when (kode[j]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                j++
            }
            hasil += pisahArgumen(kode.substring(mulai, (j - 1).coerceAtLeast(mulai)))
            i = j
        }
        return hasil
    }

    private fun pisahArgumen(isi: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var awal = 0
        isi.forEachIndexed { i, c ->
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    out += isi.substring(awal, i)
                    awal = i + 1
                }
            }
        }
        out += isi.substring(awal)
        return out.map { it.trim() }
    }

    /** `(berkas, rujukan)` untuk setiap pemanggilan `progress`/`setProgress` yang rujukannya literal. */
    private fun rujukanLiteral(): List<Pair<String, String>> =
        berkasMain().flatMap { f ->
            val k = kode(f)
            listOf("progress", "setProgress").flatMap { nama ->
                argumen(k, nama)
                    .filter { it.size >= 2 && it[1].startsWith("\"") }
                    .map { f.name to it[1] }
            }
        }

    // ---- Nilainya dibekukan ------------------------------------------------------------------

    /**
     * Rujukan ini tersimpan di perangkat dan bertahan melintasi pembaruan aplikasi. Mengubahnya
     * membuat baris lama tak terbaca: posisi baca Tahlil kembali ke nol dan tanda wudhu hilang.
     */
    @Test
    fun ejaanKunciProgresTidakBerubah() {
        assertEquals(mapOf("TAHLIL_POSISI" to "idx", "TAHLIL_HITUNGAN" to "count", "WUDHU" to "wudhu"),
            mapOf(
                "TAHLIL_POSISI" to KunciProgres.TAHLIL_POSISI,
                "TAHLIL_HITUNGAN" to KunciProgres.TAHLIL_HITUNGAN,
                "WUDHU" to KunciProgres.WUDHU,
            ))
        assertEquals("kunci progres tidak unik",
            3, setOf(KunciProgres.TAHLIL_POSISI, KunciProgres.TAHLIL_HITUNGAN, KunciProgres.WUDHU).size)
    }

    // ---- Tidak ada rujukan literal ------------------------------------------------------------

    @Test
    fun rujukanProgresTidakPernahBerupaLiteral() {
        val pelanggar = rujukanLiteral()
        assertTrue("rujukan baris progres ditulis sebagai literal, bukan dari `KunciProgres`: $pelanggar",
            pelanggar.isEmpty())
    }

    /** Pemindainya sendiri harus benar-benar menemukan pemanggilan; kalau tidak, penjaganya kosong. */
    @Test
    fun pemindaiMenemukanPemanggilanProgresYangAda() {
        val tahlil = kodeLayar("feature/tahlil/TahlilViewModel.kt")
        val rujukan = argumen(tahlil, "setProgress").mapNotNull { it.getOrNull(1) }
        assertTrue("pemindai tidak menemukan `setProgress` di TahlilViewModel", rujukan.size >= 4)
        assertTrue("pemindai salah membaca argumen `setProgress`",
            rujukan.all { it == "KunciProgres.TAHLIL_POSISI" || it == "KunciProgres.TAHLIL_HITUNGAN" })
    }

    // ---- Penulis dan pembaca memakai kunci yang sama ------------------------------------------

    @Test
    fun penulisDanPembacaProgresTahlilMemakaiKunciYangSama() {
        val vm = kodeLayar("feature/tahlil/TahlilViewModel.kt")
        assertTrue("TahlilViewModel tidak membaca posisi lewat `KunciProgres`",
            vm.contains("progress(KIND, KunciProgres.TAHLIL_POSISI)"))
        assertTrue("TahlilViewModel tidak membaca hitungan lewat `KunciProgres`",
            vm.contains("progress(KIND, KunciProgres.TAHLIL_HITUNGAN)"))
        assertTrue("TahlilViewModel tidak menyimpan posisi lewat `KunciProgres`",
            vm.contains("setProgress(KIND, KunciProgres.TAHLIL_POSISI,"))
        assertTrue("TahlilViewModel tidak menyimpan hitungan lewat `KunciProgres`",
            vm.contains("setProgress(KIND, KunciProgres.TAHLIL_HITUNGAN,"))

        // Pengaturan mengosongkan baris yang sama, jadi kuncinya pun harus sama persis.
        val set = kodeLayar("feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak mengosongkan posisi lewat `KunciProgres`",
            set.contains("setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_POSISI, 0)"))
        assertTrue("Pengaturan tidak mengosongkan hitungan lewat `KunciProgres`",
            set.contains("setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_HITUNGAN, 0)"))
    }

    @Test
    fun panduanWudhuMemakaiKunciBersama() {
        val wudhu = kodeLayar("feature/sholat/WudhuScreen.kt")
        assertTrue("WudhuScreen tidak membaca tanda selesai lewat `KunciProgres`",
            wudhu.contains("progress(KIND, KunciProgres.WUDHU)"))
        assertTrue("WudhuScreen tidak menyimpan tanda selesai lewat `KunciProgres`",
            wudhu.contains("setProgress(KIND, KunciProgres.WUDHU,"))
        assertTrue("jenis progres wudhu tidak datang dari `JenisBacaan`",
            wudhu.contains("private const val KIND = JenisBacaan.WUDHU"))
    }

    /**
     * Rujukan wudhu **sengaja** berbunyi sama dengan jenisnya: panduan wudhu hanya menyimpan satu
     * tanda untuk seluruh layar, jadi barisnya dinamai menurut bacaannya sendiri. Kesamaan itu
     * dikunci di sini — bukan salinan yang boleh berbeda, melainkan bentuk baris yang sudah ada di
     * perangkat pengguna. Progres Tahlil tidak boleh diperlakukan begitu: dua baris hidup di dalam
     * satu jenis, jadi rujukannya justru harus berbeda dari jenisnya.
     */
    @Test
    fun rujukanWudhuSamaDenganJenisnyaSecaraSengaja() {
        assertEquals("rujukan wudhu harus tetap sama dengan jenisnya supaya baris lama terbaca",
            JenisBacaan.WUDHU, KunciProgres.WUDHU)
        assertTrue("rujukan posisi Tahlil menyalin jenisnya",
            KunciProgres.TAHLIL_POSISI != JenisBacaan.TAHLIL)
        assertTrue("rujukan hitungan Tahlil menyalin jenisnya",
            KunciProgres.TAHLIL_HITUNGAN != JenisBacaan.TAHLIL)
    }
}
