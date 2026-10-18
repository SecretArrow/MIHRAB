package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan **cadangan teks bacaan** — "pakai hasil rujukan bersama bila ada isinya, pakai medan entri
 * itu sendiri bila tidak" — harus satu tempat.
 *
 * Aturan ini dulu hidup di **lima** tempat. Dua layar sholat mayit (Ghaib, Jenazah) memanggilnya
 * lewat `TampilanSholatKhusus.teksBacaan`, sementara tiga layar lain mengejanya sendiri sebagai
 * `Triple(it.first.ifBlank { b.arab }, …)`: Panduan Sholat, rincian langkah sholat, dan Tahlil.
 * Kelimanya hari ini menghasilkan hal yang sama — dan itulah yang membuatnya berbahaya: satu
 * perubahan aturan yang dikenakan pada satu salinan saja (mis. ikut memangkas spasi, atau memakai
 * judul entri sebagai cadangan) tidak berbunyi apa pun, dan pengguna membaca teks yang berbeda dari
 * layar yang menampilkan entri yang sama.
 *
 * Karena itu aturannya tinggal di [TeksBacaan], dan uji ini menjaga dua hal: (1) aturannya sendiri
 * benar — termasuk bahwa ketiga medan dipilih **terpisah**, sebab satu rujukan boleh memuat Arabnya
 * tetapi tidak terjemahannya — dan (2) tak ada layar yang mengejanya kembali.
 */
class TeksBacaanSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun layar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(
            KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun fitur(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private val b = Bacaan(id = "x", judul = "X",
        arab = "arab-entri", latin = "latin-entri", terjemahan = "terj-entri")

    private val t = TahlilItem(id = "y", judul = "Y",
        arab = "arab-tahlil", latin = "latin-tahlil", terjemahan = "terj-tahlil")

    // ---- Aturannya sendiri ----------------------------------------------------------------------

    /** Rujukan yang berisi menang atas medan entri — itulah gunanya menunjuk ke sumber bersama. */
    @Test
    fun rujukanMenangBilaAdaIsinya() {
        assertEquals(Triple("arab-rujukan", "latin-rujukan", "terj-rujukan"),
            TeksBacaan.pilih(b, "arab-rujukan", "latin-rujukan", "terj-rujukan"))
        assertEquals(Triple("arab-rujukan", "latin-rujukan", "terj-rujukan"),
            TeksBacaan.pilih(t, "arab-rujukan", "latin-rujukan", "terj-rujukan"))
    }

    /** Rujukan yang kosong — berkas hilang, id berganti, bundel lebih tua — jatuh ke entri sendiri. */
    @Test
    fun cadanganDipakaiBilaRujukanKosong() {
        assertEquals(Triple("arab-entri", "latin-entri", "terj-entri"),
            TeksBacaan.pilih(b, "", "", ""))
        assertEquals(Triple("arab-tahlil", "latin-tahlil", "terj-tahlil"),
            TeksBacaan.pilih(t, "", "", ""))
    }

    /**
     * Ketiga medan dipilih **sendiri-sendiri**. Satu rujukan boleh memuat Arabnya tetapi tidak
     * terjemahannya; memilih ketiganya sekaligus ("ada rujukan → pakai rujukan") akan membuang
     * terjemahan entri yang justru ada, dan itu tak berbunyi apa pun.
     */
    @Test
    fun tiapMedanDipilihSendiri() {
        assertEquals(Triple("arab-entri", "latin-rujukan", "terj-entri"),
            TeksBacaan.pilih(b, "", "latin-rujukan", ""))
        assertEquals(Triple("arab-rujukan", "latin-entri", "terj-entri"),
            TeksBacaan.pilih(b, "arab-rujukan", "", ""))
        assertEquals(Triple("arab-entri", "latin-entri", "terj-rujukan"),
            TeksBacaan.pilih(b, "", "", "terj-rujukan"))
        assertEquals(Triple("arab-tahlil", "latin-tahlil", "terj-tahlil"),
            TeksBacaan.pilih(t, "  ", "", "\n"))
    }

    // ---- Pemakaiannya satu ----------------------------------------------------------------------

    /**
     * Setiap layar yang menelusuri rujukan harus menyerahkan hasilnya ke aturan bersama. Yang
     * dijaga adalah **pasangannya**, bukan sekadar kehadiran salah satu: layar yang memanggil
     * resolver tanpa [TeksBacaan] sedang mengeja cadangannya sendiri.
     */
    @Test
    fun layarYangMenelusuriRujukanMemakaiAturanBersama() {
        val penelusur = fitur().filter { kode(it).contains("resolveArabLatinTerjemah(") }
        val offenders = penelusur.filterNot { kode(it).contains("TeksBacaan.pilih(") }
            .map { it.name }.sorted()
        assertTrue(
            "Ada layar yang menelusuri rujukan teks tanpa memakai aturan cadangan bersama " +
                "(TeksBacaan.pilih), sehingga cadangannya dieja sendiri di sana:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
        // Penjaga yang berhenti menemukan apa pun akan hijau tanpa memeriksa apa-apa: jumlah
        // penelusur yang diharapkan dipaku supaya perubahan bentuk pemanggilan tidak
        // membuatnya buta diam-diam.
        assertEquals("penjaga tidak lagi menemukan kelima layar yang menelusuri rujukan — " +
            "polanya salah", 5, penelusur.size)
    }

    /**
     * Bentuk salinannya tidak boleh hidup lagi di mana pun. Yang dicari adalah bentuknya, bukan
     * nama variabelnya: `ifBlank { b.arab }`, `ifBlank { cur.latin }`, dan seterusnya — dengan
     * `it.first` di antaranya seperti bentuk yang dahulu diketik.
     */
    @Test
    fun cadanganTidakDiejaSendiriDiLayarManaPun() {
        val pola = Regex("""ifBlank\s*\{\s*\w+(\.\w+)?\.(arab|latin|terjemahan)\s*\}""")
        val offenders = berkasMain()
            .filter { pola.containsMatchIn(kode(it)) }
            .map { it.name }.sorted()
        assertEquals(
            "aturan cadangan teks bacaan dieja kembali di luar TeksBacaan.kt",
            emptyList<String>(), offenders,
        )
    }

    /** Rumah lamanya tidak boleh menyimpannya lagi, supaya aturannya tidak punya dua rumah. */
    @Test
    fun rumahLamanyaTidakLagiMenyimpanAturanItu() {
        val inti = layar("core/sholat/TampilanSholatKhusus.kt")
        assertFalse(
            "TampilanSholatKhusus masih mendefinisikan teksBacaan — aturan cadangan teks punya " +
                "dua rumah lagi",
            inti.contains("fun teksBacaan"),
        )
        // Dan kedua layar mayit benar-benar memakai rumah yang baru, bukan sekadar kehilangan
        // pemanggilan lamanya.
        listOf("feature/sholat/GhaibScreen.kt", "feature/sholat/JenazahScreen.kt").forEach { rel ->
            assertTrue("$rel tidak memakai TeksBacaan.pilih untuk teks bacaan mayit",
                layar(rel).contains("TeksBacaan.pilih("))
        }
    }
}
